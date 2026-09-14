package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.event.PaymentSettledEvent;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.parking.service.EdgeLaneWaitService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 缴费入账成功后，向边缘下发开闸指令 {@code edge.gate.command/1}。
 *
 * <p>欠费拦截不上报离场：等待来自识别算费（带通道编码）。优先开仍在 15 分钟内等待该车牌的通道；
 * 没有等待时回退为向本缴款单涉及车场绑定的节点发车牌开闸，由边缘按本地最新欠费拦截识别匹配。
 * 必须在事务提交之后发布，避免边缘回查欠费仍看到未入账金额。</p>
 */
@Component
public class EdgeGateCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(EdgeGateCommandPublisher.class);

    private final EdgeLaneWaitService laneWait;
    private final ParkingOrderRepository orders;
    private final ParkingLotRepository lots;
    private final EdgeMqttConfigService mqttConfig;
    private final EdgeMqttConnectionManager mqtt;
    private final ObjectMapper objectMapper;

    public EdgeGateCommandPublisher(EdgeLaneWaitService laneWait,
                                    ParkingOrderRepository orders,
                                    ParkingLotRepository lots,
                                    EdgeMqttConfigService mqttConfig,
                                    EdgeMqttConnectionManager mqtt,
                                    ObjectMapper objectMapper) {
        this.laneWait = laneWait;
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
        List<ParkingLane> waiting = laneWait.findPayableWaitLanes(event.plateNumber(), event.plateColor());
        String prefix = EdgeMqttConfigOptions.commandPublishPrefix(config.getCommandPublishTopic());
        if (!waiting.isEmpty()) {
            for (ParkingLane lane : waiting) {
                ParkingLot lot = lane.getLot();
                String nodeCode = lot.getEdgeNodeCode().trim();
                if (!isTopicSafe(nodeCode)) {
                    log.warn("缴费开闸跳过非法节点编号 lane={} nodeCode={}", lane.getCode(), nodeCode);
                    continue;
                }
                if (publishOpen(prefix, nodeCode, lot.getCode(), lane, event, config.getQos())) {
                    laneWait.clearWait(lane.getId());
                }
            }
            return;
        }
        List<ParkingLot> fallbackLots = lotsForPayment(event.payNo());
        if (fallbackLots.isEmpty()) {
            log.info("缴费开闸无匹配通道（算费未登记等待，缴款单也无绑定节点） payNo={} plate={}",
                    event.payNo(), event.plateNumber());
            return;
        }
        for (ParkingLot lot : fallbackLots) {
            String nodeCode = lot.getEdgeNodeCode().trim();
            if (!isTopicSafe(nodeCode)) {
                log.warn("缴费开闸跳过非法节点编号 lot={} nodeCode={}", lot.getCode(), nodeCode);
                continue;
            }
            publishOpen(prefix, nodeCode, lot.getCode(), null, event, config.getQos());
        }
    }

    private List<ParkingLot> lotsForPayment(String payNo) {
        Map<Long, ParkingLot> unique = new LinkedHashMap<>();
        for (ParkingOrder order : orders.findByPaymentNoOrderByIdAsc(payNo)) {
            if (order.getLotId() == null || unique.containsKey(order.getLotId())) {
                continue;
            }
            lots.findById(order.getLotId()).ifPresent(lot -> {
                if (StringUtils.hasText(lot.getEdgeNodeCode()) && StringUtils.hasText(lot.getCode())) {
                    unique.put(lot.getId(), lot);
                }
            });
        }
        return new ArrayList<>(unique.values());
    }

    private boolean publishOpen(String prefix, String nodeCode, String lotCode,
                                ParkingLane lane, PaymentSettledEvent event, int qos) {
        String topic = prefix + "/" + nodeCode;
        if (topic.length() > EdgeMqttConfigOptions.MAX_TOPIC_LENGTH) {
            log.warn("缴费开闸主题超长，跳过 node={} topicLen={}", nodeCode, topic.length());
            return false;
        }
        byte[] payload = serialize(nodeCode, lotCode, lane, event);
        if (payload == null) {
            return false;
        }
        if (!mqtt.publish(topic, payload, qos, false)) {
            log.warn("缴费开闸 MQTT 发布失败 topic={} payNo={} plate={} lane={}",
                    topic, event.payNo(), event.plateNumber(), lane == null ? null : lane.getCode());
            return false;
        }
        log.info("已下发缴费开闸指令 topic={} payNo={} plate={} lot={} lane={}",
                topic, event.payNo(), event.plateNumber(), lotCode, lane == null ? null : lane.getCode());
        return true;
    }

    private byte[] serialize(String nodeCode, String lotCode, ParkingLane lane, PaymentSettledEvent event) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", EdgeGateCommandProtocol.SCHEMA);
            root.put("edgeCode", nodeCode);
            root.put("commandId", UUID.randomUUID().toString());
            root.put("command", EdgeGateCommandProtocol.COMMAND_OPEN);
            root.put("reason", EdgeGateCommandProtocol.REASON_PAYMENT);
            root.put("plate", event.plateNumber());
            PlateColor color = event.plateColor();
            if (color == null && lane != null) {
                color = lane.getWaitPlateColor();
            }
            if (color != null) {
                root.put("plateColor", color.name());
            }
            root.put("lotCode", lotCode);
            if (lane != null && StringUtils.hasText(lane.getCode())) {
                root.put("laneCode", lane.getCode());
            }
            if (lane != null && StringUtils.hasText(lane.getWaitRecognitionId())) {
                root.put("recognitionId", lane.getWaitRecognitionId());
            }
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
