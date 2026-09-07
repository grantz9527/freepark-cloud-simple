package com.freepark.cloud.simple.settings.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import com.freepark.cloud.simple.settings.support.EdgeMqttConfigOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 边缘配置下发调度器（纯周期全量）：按配置的同步周期，把每个已启用车场的
 * 自身配置作为一次完整快照发布到其专属主题 “{前缀}/{车场编码}”。
 *
 * <p>负载信封：{@code {"schema":"edge.config.sync/1","parkCode":..,"snapshotId":..,
 * "version":1,"generatedAt":..,"data":{...}}}，消息带 retain 标记，便于边缘侧
 * 新订阅者订阅后立即拿到最近一次完整快照（冷启动快速对齐）。</p>
 *
 * <p>调度采用 5 秒粗粒度 tick + 周期阈值判断：周期/主题可在运行期随时调整且立即生效；
 * 未启用或未配置前缀时 tick 为空操作。</p>
 */
@Component
public class EdgeConfigSyncDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EdgeConfigSyncDispatcher.class);

    /** 负载信封 schema（data 片段格式约定 v1） */
    private static final String SCHEMA = "edge.config.sync/1";
    private static final int SCHEMA_VERSION = 1;

    /** tick 扫描粒度（毫秒）；实际按配置周期决定是否下发 */
    private static final long TICK_MILLIS = 5_000L;
    /** 连接自愈对齐的节流间隔（毫秒），避免未连接时每个 tick 都读配置 */
    private static final long RECONCILE_COOLDOWN_MILLIS = 30_000L;

    private final EdgeMqttConfigService configService;
    private final EdgeMqttConnectionManager connectionManager;
    private final List<EdgeSyncTargetSource> targetSources;
    private final List<EdgeConfigSnapshotBuilder> snapshotBuilders;
    private final ObjectMapper objectMapper;

    private volatile long lastDispatchAtMillis;
    private volatile long lastReconcileAtMillis;

    public EdgeConfigSyncDispatcher(EdgeMqttConfigService configService,
            EdgeMqttConnectionManager connectionManager,
            List<EdgeSyncTargetSource> targetSources,
            List<EdgeConfigSnapshotBuilder> snapshotBuilders,
            ObjectMapper objectMapper) {
        this.configService = configService;
        this.connectionManager = connectionManager;
        this.targetSources = targetSources;
        this.snapshotBuilders = snapshotBuilders;
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
            long intervalMillis = config.getConfigSyncIntervalSeconds() * 1_000L;
            long now = System.currentTimeMillis();
            if (now - lastDispatchAtMillis < intervalMillis) {
                return;
            }
            int published = dispatchAll(config, prefix);
            lastDispatchAtMillis = now;
            log.info("边缘配置同步完成：前缀={}，下发 {} 个车场", prefix, published);
        } catch (RuntimeException e) {
            log.warn("边缘配置同步 tick 异常：{}",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
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

    private int dispatchAll(EdgeMqttConfig config, String prefix) {
        String snapshotId = UUID.randomUUID().toString();
        String generatedAt = Instant.now().toString();
        int qos = config.getQos();

        // 按车场编码去重：多个来源重复枚举同一车场时仅下发一次（保留首个构建成功的数据）
        Map<String, String> dataByCode = new LinkedHashMap<>();
        for (EdgeSyncTargetSource source : targetSources) {
            for (EdgeSyncTarget target : source.targets()) {
                String code = target.parkCode();
                if (code == null || code.isBlank() || dataByCode.containsKey(code)) {
                    continue;
                }
                String dataJson = buildDataJson(code);
                if (dataJson != null && !dataJson.isBlank()) {
                    dataByCode.put(code, dataJson);
                }
            }
        }

        int published = 0;
        int skippedTooLong = 0;
        for (Map.Entry<String, String> entry : dataByCode.entrySet()) {
            String topic = prefix + "/" + entry.getKey();
            if (topic.length() > EdgeMqttConfigOptions.MAX_TOPIC_LENGTH) {
                skippedTooLong++;
                log.warn("边缘配置同步跳过车场 {}：拼接后的主题超长（{}）", entry.getKey(), topic.length());
                continue;
            }
            byte[] payload = buildEnvelope(entry.getKey(), snapshotId, generatedAt, entry.getValue());
            if (payload == null) {
                continue;
            }
            if (connectionManager.publish(topic, payload, qos, true)) {
                published++;
            }
        }
        if (skippedTooLong > 0) {
            log.warn("边缘配置同步：因主题超长跳过 {} 个车场", skippedTooLong);
        }
        return published;
    }

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

    private byte[] buildEnvelope(String parkCode, String snapshotId, String generatedAt,
            String dataJson) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", SCHEMA);
            root.put("parkCode", parkCode);
            root.put("snapshotId", snapshotId);
            root.put("version", SCHEMA_VERSION);
            root.put("generatedAt", generatedAt);
            // data 由实现方保证是合法 JSON，解析后作为子节点挂载，避免二次转义
            root.set("data", objectMapper.readTree(dataJson));
            return objectMapper.writeValueAsBytes(root);
        } catch (Exception e) {
            log.warn("边缘配置信封序列化失败 parkCode={}：{}", parkCode,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return null;
        }
    }
}
