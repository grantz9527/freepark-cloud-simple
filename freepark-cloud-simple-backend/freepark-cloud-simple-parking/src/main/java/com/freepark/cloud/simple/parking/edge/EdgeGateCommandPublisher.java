package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.ParkingOrderStatus;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.event.PaymentSettledEvent;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.runtime.EdgeGateCommandProtocol;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 缴费入账成功后，向相关边缘节点 MQTT 下发开闸指令 {@code edge.gate.command/1}。
 *
 * <p>必须在事务提交之后发布，避免边缘收到指令后立刻回查欠费仍看到未入账金额。
 * 主题 {@code {commandPublishPrefix}/{nodeCode}}，由车场 {@code edgeNodeCode} 定位节点；
 * 未绑定边缘节点的车场跳过。</p>
 */
@Component
public class EdgeGateCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(EdgeGateCommandPublisher.class);

    private final ParkingOrderRepository orders;
    private final ParkingLotRepository lots;
    private final EdgeMqttConfigService mqttConfig;
    private final EdgeMqttConnectionManager mqtt;
    private final ObjectMapper objectMapper;

    public EdgeGateCommandPublisher(ParkingOrderRepository orders,
                                    ParkingLotRepository lots,
                                    EdgeMqttConfigService mqttConfig,
                                    EdgeMqttConnectionManager mqtt,
                                    ObjectMapper objectMapper) {
        this.orders = orders;
        this.lots = lots;
        this.mqttConfig = mqttConfig;
        this.mqtt = mqtt;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSettled(PaymentSettledEvent event) {
        if (event == null || !StringUtils.hasText(event.payNo())) {
            return;
        }
        try {
            publishForPayment(event);
        } catch (RuntimeException ex) {
            log.warn("缴费开闸指令下发异常 payNo={}：{}", event.payNo(), ex.getMessage());
        }
    }

    private void publishForPayment(PaymentSettledEvent event) {
        EdgeMqttConfig config = mqttConfig.runtimeConfig();
        if (config == null || !config.isEnabled()) {
            log.debug("边缘 MQTT 未启用，跳过缴费开闸 payNo={}", event.payNo());
            return;
        }
        if (!mqtt.isReady()) {
            log.warn("边缘 MQTT 未连接，跳过缴费开闸 payNo={} plate={}", event.payNo(), event.plateNumber());
            return;
        }
        String prefix = EdgeMqttConfigOptions.commandPublishPrefix(config.getCommandPublishTopic());
        List<ParkingOrder> paid = orders.findByPaymentNoAndStatus(event.payNo(), ParkingOrderStatus.PAID);
        Map<String, ParkingLot> lotsByKey = new LinkedHashMap<>();
        for (ParkingOrder order : paid) {
            Long lotId = order.getLotId();
            if (lotId == null) {
                continue;
            }
            ParkingLot lot = lots.findById(lotId).orElse(null);
            if (lot == null || !StringUtils.hasText(lot.getEdgeNodeCode()) || !StringUtils.hasText(lot.getCode())) {
                continue;
            }
            String nodeCode = lot.getEdgeNodeCode().trim();
            if (!isTopicSafe(nodeCode)) {
                log.warn("缴费开闸跳过非法节点编号 lot={} nodeCode={}", lot.getCode(), nodeCode);
                continue;
            }
            lotsByKey.putIfAbsent(nodeCode + "\n" + lot.getCode(), lot);
        }
        if (lotsByKey.isEmpty()) {
            log.info("缴费开闸无绑定边缘节点的车场 payNo={} plate={}", event.payNo(), event.plateNumber());
            return;
        }
        for (ParkingLot lot : lotsByKey.values()) {
            publishOpen(prefix, lot.getEdgeNodeCode().trim(), lot.getCode(), event, config.getQos());
        }
    }

    private void publishOpen(String prefix, String nodeCode, String lotCode,
                             PaymentSettledEvent event, int qos) {
        String topic = prefix + "/" + nodeCode;
        if (topic.length() > EdgeMqttConfigOptions.MAX_TOPIC_LENGTH) {
            log.warn("缴费开闸主题超长，跳过 node={} topicLen={}", nodeCode, topic.length());
            return;
        }
        byte[] payload = serialize(nodeCode, lotCode, event);
        if (payload == null) {
            return;
        }
        if (!mqtt.publish(topic, payload, qos, false)) {
            log.warn("缴费开闸 MQTT 发布失败 topic={} payNo={} plate={}", topic, event.payNo(), event.plateNumber());
            return;
        }
        log.info("已下发缴费开闸指令 topic={} payNo={} plate={} lot={}",
                topic, event.payNo(), event.plateNumber(), lotCode);
    }

    private byte[] serialize(String nodeCode, String lotCode, PaymentSettledEvent event) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", EdgeGateCommandProtocol.SCHEMA);
            root.put("edgeCode", nodeCode);
            root.put("commandId", UUID.randomUUID().toString());
            root.put("command", EdgeGateCommandProtocol.COMMAND_OPEN);
            root.put("reason", EdgeGateCommandProtocol.REASON_PAYMENT);
            root.put("plate", event.plateNumber());
            PlateColor color = event.plateColor();
            if (color != null) {
                root.put("plateColor", color.name());
            }
            root.put("lotCode", lotCode);
            root.put("payNo", event.payNo());
            root.put("issuedAt", Instant.now().toString());
            return objectMapper.writeValueAsBytes(root);
        } catch (Exception ex) {
            log.warn("缴费开闸负载序列化失败 payNo={}：{}", event.payNo(), ex.getMessage());
            return null;
        }
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
