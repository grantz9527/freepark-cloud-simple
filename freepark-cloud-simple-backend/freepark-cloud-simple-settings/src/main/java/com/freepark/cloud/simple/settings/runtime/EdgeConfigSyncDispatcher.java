package com.freepark.cloud.simple.settings.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import com.freepark.cloud.simple.settings.support.EdgeMqttConfigOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 边缘配置下发调度器（v3 全量快照）：把每个已启用的边缘节点及其应管辖车场的完整配置，
 * 以 edge.config.sync/3 的“分帧”形式发布到节点专属主题“{前缀}/{节点编号}”。
 *
 * <p>v3 帧组织（不再使用 v2 的单条 retain 大包，规避大负载与冷启动读旧数据问题）：
 * <ul>
 *   <li>为每个节点生成同一次快照的一组有序帧（同一 snapshotId），帧序 = 车场序 →
 *       车场域序（lot → blacklist → pattern → whitelist → internal → space → lane）→ 分片序；</li>
 *   <li>每帧字段：schema/edgeCode/snapshotId/version/generatedAt/kind/seq/total/lot/domain/items；
 *       条目超上限的域自动按 {@link EdgeConfigSyncProtocol#MAX_ITEMS_PER_FRAME} 切片；</li>
 *   <li>发布不带 retain；边缘侧先聚合 seq==total 再整批替换，中途缺帧则等待下一轮；
 *       节点未绑定任何车场时仍发布 1 帧空快照，供边缘侧清理已摘除的本地车场配置；</li>
 *   <li>每次下发前先向该主题发布一条空 retain 消息，清除历史上 v2 遗留的旧快照。</li>
 * </ul></p>
 *
 * <p>下发时机：
 * <ul>
 *   <li>周期全量：5 秒粗粒度 tick + 配置周期阈值（默认 24h，可在运行期调整）；</li>
 *   <li>心跳恢复补全量：监控中的节点由“离线转在线”时，立即对该节点补一次全量，
 *       避免刚恢复的边缘侧带着旧数据长期运行（配置未变化时不再周期下发）；</li>
 *   <li>手动触发：{@link #fullSyncNow()} 供管理端“立即同步”调用，跳过周期节流。</li>
 * </ul>
 * 单节点的帧序依赖同一条长连接保证有序；不同下发路径经 {@link #dispatchLock} 串行化。</p>
 */
@Component
public class EdgeConfigSyncDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EdgeConfigSyncDispatcher.class);

    /** 当前负载信封 schema（v3：分帧全量快照，不再 retain） */
    private static final String SCHEMA = EdgeConfigSyncProtocol.SCHEMA;
    private static final int SCHEMA_VERSION = EdgeConfigSyncProtocol.SCHEMA_VERSION;

    /** tick 扫描粒度（毫秒）；实际按配置周期决定是否下发 */
    private static final long TICK_MILLIS = 5_000L;
    /** 连接自愈对齐的节流间隔（毫秒），避免未连接时每个 tick 都读配置 */
    private static final long RECONCILE_COOLDOWN_MILLIS = 30_000L;

    private final EdgeMqttConfigService configService;
    private final EdgeMqttConnectionManager connectionManager;
    private final EdgeHeartbeatReceiver heartbeatReceiver;
    private final List<EdgeSyncTargetSource> targetSources;
    private final List<EdgeConfigSnapshotBuilder> snapshotBuilders;
    /** 各可同步业务域的条目构建器，按 @Order 保证同一车场的域帧顺序稳定 */
    private final List<EdgeConfigDomainSnapshotProvider> domainProviders;
    private final ObjectMapper objectMapper;

    /** 各节点上一 tick 的在线状态（用于离线→在线跃迁检测触发补全量） */
    private final Map<String, Boolean> heartbeatOnlineByNode = new HashMap<>();

    /** 全量下发的串行锁：周期/心跳恢复/手动触发不得交错分帧 */
    private final Object dispatchLock = new Object();

    private volatile long lastDispatchAtMillis;
    private volatile long lastReconcileAtMillis;

    public EdgeConfigSyncDispatcher(EdgeMqttConfigService configService,
            EdgeMqttConnectionManager connectionManager,
            EdgeHeartbeatReceiver heartbeatReceiver,
            List<EdgeSyncTargetSource> targetSources,
            List<EdgeConfigSnapshotBuilder> snapshotBuilders,
            List<EdgeConfigDomainSnapshotProvider> domainProviders,
            ObjectMapper objectMapper) {
        this.configService = configService;
        this.connectionManager = connectionManager;
        this.heartbeatReceiver = heartbeatReceiver;
        this.targetSources = targetSources;
        this.snapshotBuilders = snapshotBuilders;
        this.domainProviders = domainProviders;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = TICK_MILLIS)
    public void syncTick() {
        try {
            EdgeMqttConfig config = configService.runtimeConfig();
            if (!config.isEnabled()) {
                return;
            }
            String prefix = config.getConfigSyncPublishTopic();
            if (prefix == null || prefix.isBlank()) {
                log.debug("边缘配置同步跳过：未配置发布主题前缀");
                return;
            }
            if (!ensureReady()) {
                return;
            }
            // 心跳恢复的节点即时补一次全量（先于周期判断，避免恢复后长时间等下一轮）
            int recovered = dispatchRecoveredNodes(config, prefix);
            if (recovered > 0) {
                log.info("边缘配置同步：{} 个节点由离线转为在线，已即时下发全量快照", recovered);
            }
            long intervalMillis = config.getConfigSyncIntervalSeconds() * 1_000L;
            long now = System.currentTimeMillis();
            if (now - lastDispatchAtMillis < intervalMillis) {
                return;
            }
            DispatchSummary summary = dispatchAll(config, prefix);
            lastDispatchAtMillis = now;
            log.info("边缘配置同步完成：前缀={}，下发 {} 个节点共 {} 帧",
                    prefix, summary.nodes(), summary.frames());
        } catch (RuntimeException e) {
            log.warn("边缘配置同步 tick 异常：{}",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /**
     * 手动触发一次全量同步（管理端“立即同步”）：跳过周期节流，对全部目标节点下发快照。
     *
     * @return 本次实际下发的节点数与帧数（未启用/未就绪时返回空结果）
     */
    public DispatchSummary fullSyncNow() {
        synchronized (dispatchLock) {
            EdgeMqttConfig config = configService.runtimeConfig();
            if (!config.isEnabled()) {
                return DispatchSummary.empty();
            }
            String prefix = config.getConfigSyncPublishTopic();
            if (prefix == null || prefix.isBlank() || !ensureReady()) {
                return DispatchSummary.empty();
            }
            return dispatchAll(config, prefix);
        }
    }

    /**
     * 对单个边缘节点即时补一次全量快照（心跳恢复/车场绑定变更等场景使用）。
     *
     * @return 本次实际下发的节点数（0 或 1）与帧数
     */
    public DispatchSummary syncNodeNow(String nodeCode) {
        synchronized (dispatchLock) {
            EdgeMqttConfig config = configService.runtimeConfig();
            if (!config.isEnabled() || nodeCode == null || nodeCode.isBlank()) {
                return DispatchSummary.empty();
            }
            String prefix = config.getConfigSyncPublishTopic();
            if (prefix == null || prefix.isBlank() || !ensureReady()) {
                return DispatchSummary.empty();
            }
            List<String> parkCodes = collectTargets().get(nodeCode);
            if (parkCodes == null) {
                log.debug("边缘配置同步：节点 {} 不在当前同步目标中，跳过", nodeCode);
                return DispatchSummary.empty();
            }
            int frames = publishNodeSnapshotSafe(config, prefix, nodeCode, parkCodes);
            return frames > 0 ? new DispatchSummary(1, frames) : DispatchSummary.empty();
        }
    }

    /**
     * 变更增量下发：把某车场某业务域的一批变更条目（entries，见
     * {@link EdgeConfigSyncProtocol#KIND_DELTA}）推给当前管理该车场的全部节点。
     *
     * <p>条目按帧上限分片为多帧（同一 snapshotId、kind=delta），边缘聚齐 total 帧后
     * 逐条按云端主键应用；单条变更即单帧（total=1）。车场当前不在任何同步目标内时
     * 直接跳过（对应节点上线/绑定后由全量补齐）。</p>
     *
     * @param entries 非空数组；每元素为变更对象：upsert 带 item 全量条目，delete 带 id
     * @return 实际下发的节点数与帧数
     */
    public DispatchSummary publishDomainDelta(String parkCode, String domain, ArrayNode entries) {
        if (parkCode == null || parkCode.isBlank() || domain == null || domain.isBlank()
                || entries == null || entries.isEmpty()) {
            return DispatchSummary.empty();
        }
        synchronized (dispatchLock) {
            EdgeMqttConfig config = configService.runtimeConfig();
            if (!config.isEnabled()) {
                return DispatchSummary.empty();
            }
            String prefix = config.getConfigSyncPublishTopic();
            if (prefix == null || prefix.isBlank() || !ensureReady()) {
                return DispatchSummary.empty();
            }
            List<String> nodeCodes = collectTargets().entrySet().stream()
                    .filter(entry -> entry.getValue().contains(parkCode))
                    .map(Map.Entry::getKey)
                    .toList();
            if (nodeCodes.isEmpty()) {
                log.debug("边缘配置同步：车场 {} 域 {} 变更但无节点管理该车场，跳过增量下发", parkCode, domain);
                return DispatchSummary.empty();
            }
            int nodes = 0;
            int frames = 0;
            for (String nodeCode : nodeCodes) {
                String topic = prefix + "/" + nodeCode;
                if (topic.length() > EdgeMqttConfigOptions.MAX_TOPIC_LENGTH) {
                    log.warn("边缘配置同步跳过节点 {}：拼接后的主题超长（{}）", nodeCode, topic.length());
                    continue;
                }
                int published = publishDeltaFrames(nodeCode, parkCode, domain, entries, topic, config.getQos());
                if (published > 0) {
                    nodes++;
                }
                frames += published;
            }
            return new DispatchSummary(nodes, frames);
        }
    }

    /**
     * 车场↔节点绑定关系变化（绑/解绑、停用/启用、删除节点）事务提交后的处理：
     * 仍属同步目标的节点即时补全量；已摘除的节点（停用/删除）则下发空快照，
     * 供边缘侧清理本地残留配置，避免长期静默携带旧数据。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNodeBindingChanged(EdgeNodeBindingChangedEvent event) {
        if (event == null || event.nodeCodes() == null) {
            return;
        }
        for (String nodeCode : event.nodeCodes()) {
            try {
                resyncOrClearNode(nodeCode);
            } catch (RuntimeException e) {
                log.warn("边缘配置同步：节点 {} 绑定变更处理异常：{}", nodeCode,
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }
    }

    /** 绑定变更后处理单节点：仍在目标则补全量，已摘除则下发空快照清理 */
    private void resyncOrClearNode(String nodeCode) {
        EdgeMqttConfig config = configService.runtimeConfig();
        if (!config.isEnabled() || nodeCode == null || nodeCode.isBlank()) {
            return;
        }
        String prefix = config.getConfigSyncPublishTopic();
        if (prefix == null || prefix.isBlank() || !ensureReady()) {
            return;
        }
        String topic = prefix + "/" + nodeCode;
        if (topic.length() > EdgeMqttConfigOptions.MAX_TOPIC_LENGTH) {
            log.warn("边缘配置同步跳过节点 {}：拼接后的主题超长（{}）", nodeCode, topic.length());
            return;
        }
        synchronized (dispatchLock) {
            List<String> parkCodes = collectTargets().get(nodeCode);
            // 节点仍属同步目标（已启用，含 0 个车场的空目标）→ 全量；否则按“空目标”清理
            List<String> effective = parkCodes != null ? parkCodes : List.of();
            if (parkCodes != null) {
                log.info("边缘配置同步：节点 {} 绑定关系变化，立即下发全量快照", nodeCode);
            } else {
                log.info("边缘配置同步：节点 {} 已从同步目标摘除，下发空快照清理边缘配置", nodeCode);
            }
            publishNodeSnapshotSafe(config, prefix, nodeCode, effective);
        }
    }

    /**
     * 把一个车场某域的变更条目按帧上限切片为若干 kind=delta 帧并发布。
     *
     * @return 成功发布的帧数；任一分帧失败即中止本轮
     */
    private int publishDeltaFrames(String nodeCode, String parkCode, String domain,
            ArrayNode entries, String topic, int qos) {
        String snapshotId = UUID.randomUUID().toString();
        String generatedAt = Instant.now().toString();
        int size = entries.size();
        int total = (size + EdgeConfigSyncProtocol.MAX_ITEMS_PER_FRAME - 1)
                / EdgeConfigSyncProtocol.MAX_ITEMS_PER_FRAME;
        int published = 0;
        for (int seq = 1; seq <= total; seq++) {
            int from = (seq - 1) * EdgeConfigSyncProtocol.MAX_ITEMS_PER_FRAME;
            int to = Math.min(from + EdgeConfigSyncProtocol.MAX_ITEMS_PER_FRAME, size);
            ArrayNode part = objectMapper.createArrayNode();
            for (int i = from; i < to; i++) {
                part.add(entries.get(i));
            }
            byte[] payload = serializeDeltaFrame(nodeCode, parkCode, domain, part,
                    snapshotId, generatedAt, seq, total);
            if (payload == null) {
                break;
            }
            if (!connectionManager.publish(topic, payload, qos, false)) {
                log.warn("边缘配置同步节点 {} 增量第 {}/{} 帧发布失败，中止本轮（下次全量补齐）",
                        nodeCode, seq, total);
                break;
            }
            published++;
        }
        if (published == total) {
            log.info("边缘配置同步节点 {}：车场 {} 域 {} 增量 {} 条共 {} 帧已下发",
                    nodeCode, parkCode, domain, size, total);
        }
        return published;
    }

    /** 序列化一条 kind=delta 的变更帧（携带该帧切片内的变更条目数组） */
    private byte[] serializeDeltaFrame(String nodeCode, String parkCode, String domain,
            ArrayNode entries, String snapshotId, String generatedAt, int seq, int total) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", SCHEMA);
            root.put("edgeCode", nodeCode);
            root.put("snapshotId", snapshotId);
            root.put("version", SCHEMA_VERSION);
            root.put("generatedAt", generatedAt);
            root.put("kind", EdgeConfigSyncProtocol.KIND_DELTA);
            root.put("seq", seq);
            root.put("total", total);
            root.put("lot", parkCode);
            root.put("domain", domain);
            root.set("items", entries);
            return objectMapper.writeValueAsBytes(root);
        } catch (Exception e) {
            log.warn("边缘配置增量信封序列化失败 edgeCode={} lot={} domain={} seq={}/{}：{}",
                    nodeCode, parkCode, domain, seq, total,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return null;
        }
    }

    /** 全量下发结果汇总 */
    public record DispatchSummary(int nodes, int frames) {
        static DispatchSummary empty() {
            return new DispatchSummary(0, 0);
        }
    }

    /** 确保长连接就绪；未就绪时按节流周期尝试一次自愈对齐 */
    private boolean ensureReady() {
        if (connectionManager.isReady()) {
            lastReconcileAtMillis = System.currentTimeMillis();
            return true;
        }
        if (System.currentTimeMillis() - lastReconcileAtMillis >= RECONCILE_COOLDOWN_MILLIS) {
            connectionManager.reconcile();
            lastReconcileAtMillis = System.currentTimeMillis();
        }
        return connectionManager.isReady();
    }

    /** 心跳监控中由“离线→在线”跃迁的节点逐个即时补全量，返回触发下发的节点数 */
    private int dispatchRecoveredNodes(EdgeMqttConfig config, String prefix) {
        String monitorFilter = config.getHeartbeatSubscribeTopic();
        if (monitorFilter == null || monitorFilter.isBlank()) {
            heartbeatOnlineByNode.clear();
            return 0;
        }
        synchronized (dispatchLock) {
            Map<String, List<String>> codesByNode = collectTargets();
            if (codesByNode.isEmpty()) {
                return 0;
            }
            int triggered = 0;
            for (Map.Entry<String, List<String>> entry : codesByNode.entrySet()) {
                String nodeCode = entry.getKey();
                boolean online = isHeartbeatFresh(config, nodeCode);
                boolean previous = heartbeatOnlineByNode.getOrDefault(nodeCode, false);
                heartbeatOnlineByNode.put(nodeCode, online);
                if (online && !previous) {
                    int frames = publishNodeSnapshotSafe(config, prefix, nodeCode, entry.getValue());
                    if (frames > 0) {
                        triggered++;
                    }
                }
            }
            return triggered;
        }
    }

    private boolean isHeartbeatFresh(EdgeMqttConfig config, String nodeCode) {
        Instant last = heartbeatReceiver.lastSeen(nodeCode);
        return last != null
                && !last.plusSeconds(config.getHeartbeatOfflineSeconds()).isBefore(Instant.now());
    }

    /**
     * 对全部目标节点各下发一次 v3 全量快照。
     *
     * @return 下发的节点数与帧数
     */
    private DispatchSummary dispatchAll(EdgeMqttConfig config, String prefix) {
        Map<String, List<String>> codesByNode = collectTargets();
        if (codesByNode.isEmpty()) {
            return DispatchSummary.empty();
        }
        synchronized (dispatchLock) {
            int nodes = 0;
            int frames = 0;
            int skippedTooLong = 0;
            for (Map.Entry<String, List<String>> entry : codesByNode.entrySet()) {
                String nodeCode = entry.getKey();
                int nodeFrames = publishNodeSnapshotSafe(config, prefix, nodeCode, entry.getValue());
                if (nodeFrames < 0) {
                    skippedTooLong++;
                    continue;
                }
                frames += nodeFrames;
                if (nodeFrames > 0) {
                    nodes++;
                }
            }
            if (skippedTooLong > 0) {
                log.warn("边缘配置同步：因主题超长跳过 {} 个节点", skippedTooLong);
            }
            return new DispatchSummary(nodes, frames);
        }
    }

    /** 按节点编号去重收集目标（含应管辖车场编码），供多个下发路径复用 */
    private Map<String, List<String>> collectTargets() {
        Map<String, List<String>> codesByNode = new LinkedHashMap<>();
        for (EdgeSyncTargetSource source : targetSources) {
            for (EdgeSyncTarget target : source.targets()) {
                String nodeCode = target.nodeCode();
                if (nodeCode == null || nodeCode.isBlank() || codesByNode.containsKey(nodeCode)) {
                    continue;
                }
                List<String> parkCodes = target.parkCodes().stream()
                        .filter(code -> code != null && !code.isBlank())
                        .distinct()
                        .toList();
                codesByNode.put(nodeCode, parkCodes);
            }
        }
        return codesByNode;
    }

    /**
     * 组装并发布单个节点的全量帧序列。
     *
     * @return 成功发布的帧数；主题超长等无法下发时返回 -1（调用方单独统计跳过）
     */
    private int publishNodeSnapshotSafe(EdgeMqttConfig config, String prefix,
            String nodeCode, List<String> parkCodes) {
        String topic = prefix + "/" + nodeCode;
        if (topic.length() > EdgeMqttConfigOptions.MAX_TOPIC_LENGTH) {
            log.warn("边缘配置同步跳过节点 {}：拼接后的主题超长（{}）", nodeCode, topic.length());
            return -1;
        }
        // 先清除该节点主题上 v2 遗留的 retain 快照（若成功则已覆盖为空）
        connectionManager.publish(topic, new byte[0], config.getQos(), true);
        return publishNodeSnapshot(nodeCode, parkCodes, topic, config.getQos());
    }

    /** 组装并发布单个节点的全量帧序列，返回成功发布的帧数；任一分帧失败即中止本轮 */
    private int publishNodeSnapshot(String nodeCode, List<String> parkCodes, String topic, int qos) {
        String snapshotId = UUID.randomUUID().toString();
        String generatedAt = Instant.now().toString();
        List<NodeFrameSpec> frames = buildNodeFrames(nodeCode, parkCodes);
        int total = frames.size();
        int published = 0;
        for (int i = 0; i < total; i++) {
            NodeFrameSpec spec = frames.get(i);
            byte[] payload = serializeFrame(spec, nodeCode, snapshotId, generatedAt,
                    i + 1, total);
            if (payload == null) {
                break;
            }
            if (!connectionManager.publish(topic, payload, qos, false)) {
                log.warn("边缘配置同步节点 {}：第 {}/{} 帧发布失败，中止本轮（等待下一周期补齐）",
                        nodeCode, i + 1, total);
                break;
            }
            published++;
        }
        if (published == total && total > 0) {
            log.info("边缘配置同步节点 {}：快照 {} 共 {} 帧全部下发", nodeCode, snapshotId, total);
        }
        return published;
    }

    /**
     * 组装节点的帧清单：车场序 × （lot 基础帧 + 各业务域分片帧）。
     * 车场构建失败时跳过该车场；节点名下没有可下发车场时返回单条空快照帧，
     * 边缘侧据此清理本地已摘除的车场配置。
     */
    private List<NodeFrameSpec> buildNodeFrames(String nodeCode, List<String> parkCodes) {
        List<NodeFrameSpec> frames = new ArrayList<>();
        boolean anyLot = false;
        for (String parkCode : parkCodes) {
            String basicDataJson = buildDataJson(parkCode);
            if (basicDataJson == null) {
                continue;
            }
            anyLot = true;
            try {
                frames.add(new NodeFrameSpec(parkCode,
                        EdgeConfigSyncProtocol.DOMAIN_LOT, singleItemArray(basicDataJson)));
                for (EdgeConfigDomainSnapshotProvider provider : domainProviders) {
                    appendDomainFrames(frames, parkCode, provider);
                }
            } catch (RuntimeException e) {
                log.warn("边缘配置同步节点 {} 车场 {} 帧组装失败：{}", nodeCode, parkCode,
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }
        if (!anyLot) {
            frames.add(new NodeFrameSpec(null, EdgeConfigSyncProtocol.DOMAIN_LOT,
                    objectMapper.createArrayNode()));
        }
        return frames;
    }

    /** 把车场某域条目按上限切片为多帧并追加到帧清单 */
    private void appendDomainFrames(List<NodeFrameSpec> frames, String parkCode,
            EdgeConfigDomainSnapshotProvider provider) {
        JsonNode node;
        try {
            String itemsText = provider.buildDomainItemsJson(parkCode);
            if (itemsText == null || itemsText.isBlank()) {
                return;
            }
            node = objectMapper.readTree(itemsText);
        } catch (Exception e) {
            throw new IllegalStateException("domain " + provider.domain() + " items parse failed: "
                    + e.getMessage(), e);
        }
        if (!node.isArray() || node.isEmpty()) {
            return;
        }
        ArrayNode items = (ArrayNode) node;
        for (int start = 0; start < items.size();
                start += EdgeConfigSyncProtocol.MAX_ITEMS_PER_FRAME) {
            int end = Math.min(start + EdgeConfigSyncProtocol.MAX_ITEMS_PER_FRAME, items.size());
            ArrayNode part = objectMapper.createArrayNode();
            for (int i = start; i < end; i++) {
                part.add(items.get(i));
            }
            frames.add(new NodeFrameSpec(parkCode, provider.domain(), part));
        }
    }

    /** 构建单个车场的基础配置片段 JSON（由车场侧 SPI 提供） */
    private String buildDataJson(String parkCode) {
        for (EdgeConfigSnapshotBuilder builder : snapshotBuilders) {
            try {
                String dataJson = builder.buildConfigDataJson(parkCode);
                if (dataJson != null && !dataJson.isBlank()) {
                    return dataJson;
                }
            } catch (RuntimeException e) {
                log.warn("边缘配置快照构建失败 parkCode={}：{}", parkCode,
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }
        return null;
    }

    private ArrayNode singleItemArray(String itemJson) {
        ArrayNode array = objectMapper.createArrayNode();
        try {
            array.add(objectMapper.readTree(itemJson));
        } catch (Exception e) {
            throw new IllegalStateException("lot config is not valid json: " + e.getMessage(), e);
        }
        return array;
    }

    private byte[] serializeFrame(NodeFrameSpec spec, String nodeCode, String snapshotId,
            String generatedAt, int seq, int total) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", SCHEMA);
            root.put("edgeCode", nodeCode);
            root.put("snapshotId", snapshotId);
            root.put("version", SCHEMA_VERSION);
            root.put("generatedAt", generatedAt);
            root.put("kind", EdgeConfigSyncProtocol.KIND_FULL);
            root.put("seq", seq);
            root.put("total", total);
            if (spec.lot() != null) {
                root.put("lot", spec.lot());
            }
            root.put("domain", spec.domain());
            root.set("items", spec.items());
            return objectMapper.writeValueAsBytes(root);
        } catch (Exception e) {
            log.warn("边缘配置信封序列化失败 edgeCode={} seq={}/{}：{}", nodeCode, seq, total,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return null;
        }
    }

    /** 单个全量帧的描述：目标车场（空=整节点空快照）、业务域、该帧条目 */
    private record NodeFrameSpec(String lot, String domain, ArrayNode items) {
    }
}
