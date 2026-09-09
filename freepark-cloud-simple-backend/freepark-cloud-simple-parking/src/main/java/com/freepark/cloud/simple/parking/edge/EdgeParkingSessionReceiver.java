package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepark.cloud.simple.billing.service.BillingSessionChargeService;
import com.freepark.cloud.simple.parking.entity.DiscountVehicle;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.ParkingOrderStatus;
import com.freepark.cloud.simple.parking.entity.ParkingPayStatus;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.repository.DiscountVehicleRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSessionRepository;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.runtime.EdgeInboundConsumer;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 上行停车流水接收器（edge → cloud）：云端订阅“上报数据订阅主题”（默认
 * {@code parking/report/#}）后，接收各边缘节点上报的 {@code edge.parking.session/1}
 * 流水快照并落库到云端 {@code parking_session}。
 *
 * <p>每条快照携带“完整当前状态”（入场/出场/作废都在同一条内体现），因此以
 * {@code edgeNodeCode + edgeSessionId} 为幂等键做 upsert：云端没有则新增，有则
 * 整体覆盖为快照状态；边缘侧重复补推（QoS 1 at-least-once + 断线重推）不会产生
 * 重复记录，后到的快照永远更新到最新状态。</p>
 *
 * <p>车场归属校验：按快照 {@code lotCode} 定位云端车场，且该车场必须绑定在
 * 主题末段 {@code nodeCode}（上报来源节点）名下，否则视为无效数据丢弃。
 * 边缘流水中的通道/识别记录为本地 UUID，云端无可对应主键，故仅落名称快照，
 * 图片按 v1 约定不在负载内。</p>
 */
@Component
public class EdgeParkingSessionReceiver implements EdgeInboundConsumer {

    private static final Logger log = LoggerFactory.getLogger(EdgeParkingSessionReceiver.class);

    /** 流水快照负载信封 schema（边缘侧 v1 上报约定） */
    public static final String SCHEMA = "edge.parking.session/1";

    private final EdgeMqttConfigService configService;
    private final ParkingSessionRepository sessionRepository;
    private final ParkingLotRepository lotRepository;
    private final DiscountVehicleRepository discountVehicles;
    private final ParkingOrderRepository orderRepository;
    private final BillingSessionChargeService chargeService;
    private final ObjectMapper objectMapper;

    public EdgeParkingSessionReceiver(EdgeMqttConfigService configService,
            ParkingSessionRepository sessionRepository,
            ParkingLotRepository lotRepository,
            DiscountVehicleRepository discountVehicles,
            ParkingOrderRepository orderRepository,
            BillingSessionChargeService chargeService,
            ObjectMapper objectMapper) {
        this.configService = configService;
        this.sessionRepository = sessionRepository;
        this.lotRepository = lotRepository;
        this.discountVehicles = discountVehicles;
        this.orderRepository = orderRepository;
        this.chargeService = chargeService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void onMessage(String topic, byte[] payload) {
        EdgeMqttConfig config = configService.runtimeConfig();
        if (!config.isEnabled()) {
            return;
        }
        String filter = config.getReportSubscribeTopic();
        if (filter == null || filter.isBlank()) {
            return;
        }
        String nodeCode = extractNodeCode(topic, filter);
        if (nodeCode == null) {
            // 不在“上报数据订阅主题”下的消息（如心跳等其它订阅）交给其它消费方
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("停车流水上报负载非法 topic={}：{}", topic, e.getMessage());
            return;
        }
        if (root == null || !root.isObject()) {
            return;
        }
        String schema = textOrNull(root.path("schema"));
        String edgeCode = textOrNull(root.path("edgeCode"));
        if (!SCHEMA.equals(schema) || !nodeCode.equals(edgeCode)) {
            log.debug("忽略非流水上报负载 topic={} schema={} edgeCode={}", topic, schema, edgeCode);
            return;
        }
        // 落库失败（RuntimeException）上抛：由连接管理器记录日志并回滚事务。
        // 注：边缘侧在 Broker 收到报文（PUBACK）后即清除待同步标记，云端落库失败
        // 只能靠日志/运维补数据，不做端到端重试（与心跳链路同一可靠性边界）。
        applySnapshot(nodeCode, root);
    }

    /** 解析快照并幂等 upsert 到云端 parking_session（失败上抛由事务回滚）。 */
    private void applySnapshot(String nodeCode, JsonNode root) {
        String edgeSessionId = textOrNull(root.path("sessionId"));
        String lotCode = textOrNull(root.path("lotCode"));
        String plateNumber = textOrNull(root.path("plateNumber"));
        String statusText = textOrNull(root.path("status"));
        String entryTimeText = textOrNull(root.path("entryTime"));
        if (edgeSessionId == null || lotCode == null || plateNumber == null
                || statusText == null || entryTimeText == null) {
            log.warn("停车流水快照缺少必填字段（sessionId/lotCode/plateNumber/status/entryTime），丢弃");
            return;
        }
        ParkingSessionStatus status = parseEnum(ParkingSessionStatus.class, statusText);
        if (status == null) {
            log.warn("停车流水快照状态非法 status={}，丢弃", statusText);
            return;
        }
        ParkingLot lot = lotRepository.findByCode(lotCode).orElse(null);
        if (lot == null) {
            log.warn("停车流水快照车场不存在 lotCode={}，丢弃", lotCode);
            return;
        }
        if (lot.getEdgeNodeCode() == null || !lot.getEdgeNodeCode().equals(nodeCode)) {
            log.warn("停车流水快照车场未绑定上报节点 lotCode={} nodeCode={}，丢弃", lotCode, nodeCode);
            return;
        }
        LocalDateTime entryTime = parseInstant(entryTimeText);
        LocalDateTime exitTime = textOrNull(root.path("exitTime")) == null
                ? null : parseInstant(root.path("exitTime").asText());
        if (entryTime == null || (root.hasNonNull("exitTime") && exitTime == null)) {
            log.warn("停车流水快照时间字段非法（entryTime/exitTime），丢弃");
            return;
        }
        ParkingSession session = sessionRepository
                .findByEdgeNodeCodeAndEdgeSessionId(nodeCode, edgeSessionId)
                .orElseGet(ParkingSession::new);
        session.setEdgeNodeCode(nodeCode);
        session.setEdgeSessionId(edgeSessionId);
        session.setLotId(lot.getId());
        session.setLotName(firstNonBlank(textOrNull(root.path("lotName")), lot.getName()));
        session.setPlateNumber(plateNumber.trim().toUpperCase());
        session.setPlateColor(parseEnum(PlateColor.class, textOrNull(root.path("plateColor"))));
        session.setStatus(status);
        session.setEntryTime(entryTime);
        session.setEntryLaneName(textOrNull(root.path("entryLaneName")));
        session.setExitTime(exitTime);
        session.setExitLaneName(textOrNull(root.path("exitLaneName")));
        if (status == ParkingSessionStatus.CLOSED && session.getPayStatus() == null) {
            // 与云端关场默认一致：已出场未登记支付前视为未支付
            session.setPayStatus(ParkingPayStatus.UNPAID);
        }
        if (status == ParkingSessionStatus.VOIDED) {
            if (session.getId() != null && session.paidAmountOrZero().signum() > 0) {
                // 该流水在云端已有收款入账：作废会抹掉真实收款记录，忽略本次作废上报
                log.warn("忽略边缘作废上报：云端流水已有收款 nodeCode={} edgeSessionId={} paidAmount={}",
                        nodeCode, edgeSessionId, session.paidAmountOrZero());
                return;
            }
            // 与云端作废语义一致：作废清空支付状态与时间
            session.setPayStatus(null);
            session.setPayTime(null);
        }
        settleFee(session);
        sessionRepository.save(session);
        if (status == ParkingSessionStatus.VOIDED && session.getId() != null) {
            cancelPendingOrders(session.getId());
        }
        log.info("已入库边缘停车流水 nodeCode={} edgeSessionId={} status={} plate={}",
                nodeCode, edgeSessionId, status, session.getPlateNumber());
    }

    /** 流水作废时把其「待支付」订单一并取消，避免金额占用残留。 */
    private void cancelPendingOrders(Long sessionId) {
        for (ParkingOrder order : orderRepository.findBySessionIdAndStatus(
                sessionId, ParkingOrderStatus.PENDING)) {
            order.setStatus(ParkingOrderStatus.CANCELLED);
            order.setPayTime(null);
            orderRepository.save(order);
        }
    }

    /**
     * 云端结算快照：边缘上报的流水费用一律以云端口径重算并落库，不沿用边缘本地金额。
     * 已出场流水按「入场 → 出场」结算应收；命中的「优惠车辆」享受每次入场免费时长
     * （整段停车落在免费时长内应收 0 元，超出部分自免费窗口末起计费）。作废清空金额；
     * 在场或缺出场时间的流水不产生结算快照。
     */
    private void settleFee(ParkingSession session) {
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            session.setFeeYuan(null);
            return;
        }
        if (session.getStatus() != ParkingSessionStatus.CLOSED
                || session.getExitTime() == null
                || session.getEntryTime() == null) {
            return;
        }
        Integer freeMinutes = null;
        String plate = session.getPlateNumber();
        if (session.getLotId() != null && plate != null && !plate.isBlank()) {
            DiscountVehicle discount = discountVehicles
                    .findByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(
                            session.getLotId(), plate)
                    .orElse(null);
            if (discount != null) {
                freeMinutes = discount.getFreeMinutes();
            }
        }
        String color = session.getPlateColor() == null
                ? null : session.getPlateColor().name();
        session.setFeeYuan(chargeService.chargeFor(session.getLotId(), color,
                session.getEntryTime(), session.getExitTime(), freeMinutes));
    }

    /** 从主题解析节点编号：编号取订阅过滤器命中后的最后一个主题层级 */
    private static String extractNodeCode(String topic, String filter) {
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

    /** 边缘侧时间统一为 ISO-8601 UTC（Instant 文本，带 Z 后缀），转为云端 UTC LocalDateTime */
    private static LocalDateTime parseInstant(String text) {
        try {
            return Instant.parse(text).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String text) {
        if (text == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText().trim();
        return value.isEmpty() ? null : value;
    }

    private static String firstNonBlank(String first, String fallback) {
        return (first != null && !first.isBlank()) ? first : fallback;
    }
}
