package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.event.ParkingSessionChangedEvent;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSessionRepository;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.runtime.EdgeMqttConnectionManager;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import com.freepark.cloud.simple.settings.support.EdgeMqttConfigOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 云端停车流水变更后，向边缘节点下发完整快照 {@code edge.parking.session/1}（origin=CLOUD）。
 * 主题与开闸指令相同：{@code {commandPublishPrefix}/{nodeCode}}。
 */
@Component
public class EdgeSessionPushPublisher {

    private static final Logger log = LoggerFactory.getLogger(EdgeSessionPushPublisher.class);

    static final String SCHEMA = "edge.parking.session/1";
    static final String ORIGIN_CLOUD = "CLOUD";

    private final ParkingSessionRepository sessions;
    private final ParkingLotRepository lots;
    private final EdgeMqttConfigService mqttConfig;
    private final EdgeMqttConnectionManager mqtt;
    private final ObjectMapper objectMapper;

    public EdgeSessionPushPublisher(ParkingSessionRepository sessions,
                                    ParkingLotRepository lots,
                                    EdgeMqttConfigService mqttConfig,
                                    EdgeMqttConnectionManager mqtt,
                                    ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.lots = lots;
        this.mqttConfig = mqttConfig;
        this.mqtt = mqtt;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionChanged(ParkingSessionChangedEvent event) {
        if (event == null || event.sessionId() == null) {
            return;
        }
        try {
            publish(event.sessionId());
        } catch (RuntimeException ex) {
            log.warn("云端流水下发异常 sessionId={}：{}", event.sessionId(), ex.getMessage());
        }
    }

    private void publish(Long sessionId) {
        ParkingSession session = sessions.findById(sessionId).orElse(null);
        if (session == null || session.getLotId() == null) {
            return;
        }
        ParkingLot lot = lots.findById(session.getLotId()).orElse(null);
        if (lot == null || !StringUtils.hasText(lot.getCode())) {
            return;
        }
        String nodeCode = firstNonBlank(session.getEdgeNodeCode(), lot.getEdgeNodeCode());
        if (!StringUtils.hasText(nodeCode) || !isTopicSafe(nodeCode.trim())) {
            log.debug("流水未绑定边缘节点，跳过下发 sessionId={} lot={}", sessionId, lot.getCode());
            return;
        }
        nodeCode = nodeCode.trim();
        EdgeMqttConfig config = mqttConfig.runtimeConfig();
        if (config == null || !config.isEnabled()) {
            log.debug("边缘 MQTT 未启用，跳过流水下发 sessionId={}", sessionId);
            return;
        }
        if (!mqtt.isReady()) {
            log.warn("边缘 MQTT 未连接，跳过流水下发 sessionId={} plate={}", sessionId, session.getPlateNumber());
            return;
        }
        String prefix = EdgeMqttConfigOptions.commandPublishPrefix(config.getCommandPublishTopic());
        String topic = prefix + "/" + nodeCode;
        if (topic.length() > EdgeMqttConfigOptions.MAX_TOPIC_LENGTH) {
            log.warn("流水下发主题超长，跳过 node={} topicLen={}", nodeCode, topic.length());
            return;
        }
        byte[] payload = serialize(nodeCode, lot.getCode(), session);
        if (payload == null) {
            return;
        }
        if (!mqtt.publish(topic, payload, config.getQos(), false)) {
            log.warn("云端流水 MQTT 发布失败 topic={} sessionId={}", topic, sessionId);
            return;
        }
        log.info("已下发云端停车流水 topic={} sessionId={} plate={} status={}",
                topic, sessionId, session.getPlateNumber(), session.getStatus());
    }

    private byte[] serialize(String nodeCode, String lotCode, ParkingSession session) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", SCHEMA);
            root.put("origin", ORIGIN_CLOUD);
            root.put("edgeCode", nodeCode);
            root.put("cloudId", session.getId());
            root.put("cloudRevision", session.getCloudRevision() == null ? 1L : session.getCloudRevision());
            if (StringUtils.hasText(session.getEdgeSessionId())) {
                root.put("sessionId", session.getEdgeSessionId().trim());
            }
            root.put("lotCode", lotCode);
            if (StringUtils.hasText(session.getLotName())) {
                root.put("lotName", session.getLotName());
            }
            root.put("plateNumber", session.getPlateNumber());
            if (session.getPlateColor() != null) {
                root.put("plateColor", session.getPlateColor().name());
            }
            root.put("status", session.getStatus().name());
            putTime(root, "entryTime", session.getEntryTime());
            putTime(root, "exitTime", session.getExitTime());
            if (StringUtils.hasText(session.getEntryLaneName())) {
                root.put("entryLaneName", session.getEntryLaneName());
            }
            if (StringUtils.hasText(session.getExitLaneName())) {
                root.put("exitLaneName", session.getExitLaneName());
            }
            root.put("issuedAt", Instant.now().toString());
            return objectMapper.writeValueAsBytes(root);
        } catch (Exception ex) {
            log.warn("云端流水负载序列化失败 sessionId={}：{}", session.getId(), ex.getMessage());
            return null;
        }
    }

    private static void putTime(ObjectNode root, String field, LocalDateTime value) {
        if (value == null) {
            return;
        }
        root.put(field, value.atOffset(ZoneOffset.UTC).toInstant().toString());
    }

    private static String firstNonBlank(String first, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return fallback;
    }

    private static boolean isTopicSafe(String value) {
        if (value.length() > 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }
}
