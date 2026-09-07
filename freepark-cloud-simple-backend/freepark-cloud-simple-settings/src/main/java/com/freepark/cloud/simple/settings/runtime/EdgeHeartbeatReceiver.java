package com.freepark.cloud.simple.settings.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上行心跳接收器：云端订阅“心跳订阅主题”后，记录各车场最近一次心跳到达时刻。
 *
 * <p>心跳消息约定：主题形如 {@code {心跳订阅主题}/{车场编码}}（默认订阅
 * {@code parking/heartbeat/#}），负载为 JSON 信封且 {@code schema=edge.heartbeat/1}；
 * 车场编码取自主题最后一个层级，到达时刻取云端本地时钟（不信任边缘时钟）。</p>
 *
 * <p>状态保持：配置停用/主题变更（订阅过滤器变化）时清空历史到点，避免跨监控期
 * 用旧数据误判在线。在线/离线判定不在此处做，由外层按“最近到达是否超阈值”动态计算。</p>
 */
@Component
public class EdgeHeartbeatReceiver implements EdgeInboundConsumer {

    private static final Logger log = LoggerFactory.getLogger(EdgeHeartbeatReceiver.class);

    /** 心跳负载信封 schema（边缘侧 v1 心跳约定） */
    public static final String SCHEMA = "edge.heartbeat/1";

    private final EdgeMqttConfigService configService;
    private final ObjectMapper objectMapper;

    /** 各车场最近一次心跳到达时刻（云端本地时钟，UTC 锚点） */
    private final ConcurrentHashMap<String, Instant> lastSeenByPark = new ConcurrentHashMap<>();
    /** 当前生效的订阅过滤器；null/空=未监控。变化时清空历史到点 */
    private volatile String activeFilter;

    public EdgeHeartbeatReceiver(EdgeMqttConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String topic, byte[] payload) {
        EdgeMqttConfig config = configService.runtimeConfig();
        if (!config.isEnabled()) {
            return;
        }
        String filter = config.getHeartbeatSubscribeTopic();
        if (filter == null || filter.isBlank()) {
            return;
        }
        String parkCode = extractParkCode(topic, filter);
        if (parkCode == null) {
            return;
        }
        if (!isHeartbeatEnvelope(payload)) {
            log.debug("忽略非心跳负载 topic={}", topic);
            return;
        }
        lastSeenByPark.put(parkCode, Instant.now());
        // 首个消息建立监控基线；配置事件未触发时不再重复清空（表此时为空或同过滤器）
        if (activeFilter == null) {
            activeFilter = filter;
        }
        log.debug("记录车场心跳 parkCode={} topic={}", parkCode, topic);
    }

    /** 配置保存成功后对齐监控状态：过滤器变化/停用即清空历史到点 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfigChanged(EdgeMqttConfigChangedEvent event) {
        refreshState();
    }

    private synchronized void refreshState() {
        EdgeMqttConfig config = configService.runtimeConfig();
        boolean enabled = config.isEnabled();
        String filter = config.getHeartbeatSubscribeTopic();
        String key = (enabled && filter != null && !filter.isBlank()) ? filter : null;
        if (Objects.equals(key, activeFilter)) {
            return;
        }
        activeFilter = key;
        if (!lastSeenByPark.isEmpty()) {
            lastSeenByPark.clear();
        }
        if (key != null) {
            log.info("已启用车场上行心跳监控：订阅 {}", key);
        } else {
            log.info("已停用车场上行心跳监控");
        }
    }

    /**
     * 返回某车场最近一次心跳到达时刻；从未收到过该车场心跳时返回 null。
     * 判定“是否监控中”请配合当前配置（enabled + 心跳订阅主题非空）。
     */
    public Instant lastSeen(String parkCode) {
        return parkCode == null ? null : lastSeenByPark.get(parkCode);
    }

    /** 当前已记录心跳的车场数（排查/日志用） */
    public int trackedCount() {
        return lastSeenByPark.size();
    }

    /** 从主题解析车场编码：编码取订阅过滤器命中后的最后一个主题层级 */
    private static String extractParkCode(String topic, String filter) {
        boolean wildcardEnd = filter.endsWith("#") || filter.endsWith("+");
        int slash = filter.lastIndexOf('/');
        if (slash <= 0) {
            return null;
        }
        if (!wildcardEnd) {
            if (!topic.equals(filter)) {
                return null;
            }
            return filter.substring(slash + 1);
        }
        String base = filter.substring(0, slash + 1);
        if (!topic.startsWith(base)) {
            return null;
        }
        String code = topic.substring(base.length());
        if (code.isEmpty() || code.indexOf('/') >= 0) {
            return null;
        }
        return code;
    }

    private boolean isHeartbeatEnvelope(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            return node != null && node.isObject()
                    && SCHEMA.equals(node.path("schema").asText(null));
        } catch (Exception e) {
            return false;
        }
    }
}
