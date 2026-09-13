package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.ParkingOrderView;
import com.freepark.cloud.simple.parking.dto.PayableQuoteView;
import com.freepark.cloud.simple.parking.dto.RefundParkingOrderRequest;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.ParkingOrderRefund;
import com.freepark.cloud.simple.parking.entity.ParkingOrderStatus;
import com.freepark.cloud.simple.parking.entity.ParkingRefundType;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.entity.PaymentMethod;
import com.freepark.cloud.simple.parking.entity.PaymentOrder;
import com.freepark.cloud.simple.parking.entity.PaymentOrderStatus;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRefundRepository;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSessionRepository;
import com.freepark.cloud.simple.parking.repository.PaymentOrderRepository;
import com.freepark.cloud.simple.parking.service.alipay.AlipayRefundClient;
import com.freepark.cloud.simple.parking.service.wechat.WeChatRefundClient;
import com.freepark.cloud.simple.settings.service.AlipayConfigService;
import com.freepark.cloud.simple.settings.service.WeChatConfigService;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 停车订单服务（停车管理 - 停车订单）：
 * 每次收费请求（缴费/登记收款）为一条停车流水生成一笔订单并记录金额，
 * 订单金额 = 当前应收 − 流水累计已支付 − 该流水未支付/待支付订单合计，
 * 保证在场车辆多次缴费「第二次仅需支付再次产生的金额」，也避免并发请求重复下单重复计费。
 * <p>订单生命周期：PENDING（待支付，金额占用可收口径）→ PAID（登记收款入账到流水累计已支付）
 * → PARTIAL_REFUND / REFUNDED（部分或全部退款：线上单先调微信/支付宝退款，再回冲流水累计已支付），
 * 或 CANCELLED（仅待支付可取消，释放占用）。
 */
@Service
public class ParkingOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ParkingOrderRepository orders;
    private final ParkingOrderRefundRepository refunds;
    private final ParkingSessionRepository sessions;
    private final ParkingSessionService sessionService;
    private final PayRecordService payRecordService;
    private final PaymentOrderRepository payments;
    private final WeChatConfigService weChatConfig;
    private final AlipayConfigService alipayConfig;
    private final WeChatRefundClient weChatRefund;
    private final AlipayRefundClient alipayRefund;
    private final AdminGuard adminGuard;
    private final SiteZoneProvider siteZoneProvider;

    public ParkingOrderService(ParkingOrderRepository orders,
                               ParkingOrderRefundRepository refunds,
                               ParkingSessionRepository sessions,
                               ParkingSessionService sessionService,
                               PayRecordService payRecordService,
                               PaymentOrderRepository payments,
                               WeChatConfigService weChatConfig,
                               AlipayConfigService alipayConfig,
                               WeChatRefundClient weChatRefund,
                               AlipayRefundClient alipayRefund,
                               AdminGuard adminGuard,
                               SiteZoneProvider siteZoneProvider) {
        this.orders = orders;
        this.refunds = refunds;
        this.sessions = sessions;
        this.sessionService = sessionService;
        this.payRecordService = payRecordService;
        this.payments = payments;
        this.weChatConfig = weChatConfig;
        this.alipayConfig = alipayConfig;
        this.weChatRefund = weChatRefund;
        this.alipayRefund = alipayRefund;
        this.adminGuard = adminGuard;
        this.siteZoneProvider = siteZoneProvider;
    }

    /** 停车订单分页列表：支持按流水/车场/关键字（订单号、车牌、车场名）/订单状态/入场日期区间筛选。 */
    @Transactional(readOnly = true)
    public PageResult<ParkingOrderView> listOrders(Long sessionId, Long lotId, String keyword,
                                                   ParkingOrderStatus status,
                                                   LocalDate startDate, LocalDate endDate,
                                                   int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Specification<ParkingOrder> spec = buildSpec(sessionId, lotId, keyword, status, startDate, endDate);
        Page<ParkingOrder> result = orders.findAll(spec,
                PageRequest.of(safePage - 1, safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        List<ParkingOrderView> items = result.getContent().stream()
                .map(ParkingOrderView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    /**
     * 创建停车订单：按「当前应收 − 累计已支付 − 待付订单」计算本次应付金额并快照留档。
     * 当前应收已全部覆盖（已付清或已有待付订单占用）时拒绝再次下单，防止重复缴费。
     */
    @Transactional
    public ParkingOrderView createOrder(Long sessionId) {
        adminGuard.requireEnabledAdmin();
        if (sessionId == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        if (session.getStatus() == ParkingSessionStatus.VOIDED
                || session.getEntryTime() == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        var quote = sessionService.payableQuote(sessionId);
        if (quote.payableYuan().signum() <= 0) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_NOT_PAYABLE);
        }
        return ParkingOrderView.from(savePendingOrder(session, quote, null));
    }

    /**
     * C 端在线缴费下单：为指定流水生成一笔「待缴」订单，并归集到指定缴款单。
     * 金额与三方快照直接采用事先算好的可缴口径（{@link ParkingSessionService#listPayableQuotes}），
     * 保证同一缴款单内多笔订单金额之和恰好等于缴款总额；无管理员校验（公开缴费流程专用）。
     */
    @Transactional
    public ParkingOrder createPendingOrder(Long sessionId, PayableQuoteView quote, String paymentNo) {
        ParkingSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        return savePendingOrder(session, quote, paymentNo);
    }

    /**
     * C 端支付成功：把指定支付单下的待支付订单全部置为「已支付」，
     * 并逐笔把订单金额入账到关联流水的累计已支付（支付状态随之自动推导）。
     */
    @Transactional
    public void settlePaymentOrders(String paymentNo, LocalDateTime payTime) {
        for (ParkingOrder order : orders.findByPaymentNoAndStatus(paymentNo, ParkingOrderStatus.PENDING)) {
            order.setStatus(ParkingOrderStatus.PAID);
            order.setPayTime(payTime);
            orders.save(order);
            sessionService.applyOrderReceivable(order.getSessionId(), order.getAmountYuan());
        }
    }

    /** C 端支付关闭（用户取消/重新发起）：释放指定支付单下的待支付订单。 */
    @Transactional
    public void cancelPaymentOrders(String paymentNo) {
        for (ParkingOrder order : orders.findByPaymentNoAndStatus(paymentNo, ParkingOrderStatus.PENDING)) {
            order.setStatus(ParkingOrderStatus.CANCELLED);
            order.setPayTime(null);
            orders.save(order);
        }
    }

    /** 生成一笔待支付订单并快照下单时的金额口径；{@code paymentNo} 为 null 表示管理端人工下单。 */
    private ParkingOrder savePendingOrder(ParkingSession session, PayableQuoteView quote, String paymentNo) {
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo(nextOrderNo());
        order.setPaymentNo(paymentNo);
        order.setSessionId(session.getId());
        order.setSessionStatus(session.getStatus());
        order.setLotId(session.getLotId());
        order.setLotName(session.getLotName());
        order.setPlateNumber(session.getPlateNumber());
        order.setPlateColor(session.getPlateColor());
        order.setEntryTime(session.getEntryTime());
        order.setReceivableYuan(quote.receivableYuan());
        order.setPaidBeforeYuan(quote.paidYuan());
        order.setPendingBeforeYuan(quote.pendingYuan());
        order.setAmountYuan(quote.payableYuan());
        order.setStatus(ParkingOrderStatus.PENDING);
        return orders.save(order);
    }

    /** 登记收款：待支付订单 → 已支付，并把订单金额累加到关联流水「累计已支付」，支付状态随之自动推导。 */
    @Transactional
    public ParkingOrderView registerPayment(Long orderId) {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        ParkingOrder order = requireOrder(orderId);
        if (order.getStatus() != ParkingOrderStatus.PENDING) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_BAD_STATE);
        }
        order.setStatus(ParkingOrderStatus.PAID);
        order.setPayTime(SiteZoneTimes.nowUtc());
        orders.save(order);
        sessionService.applyOrderReceivable(order.getSessionId(), order.getAmountYuan());
        if (!StringUtils.hasText(order.getPaymentNo())) {
            payRecordService.recordCashPay(order, operator);
        }
        return ParkingOrderView.from(order);
    }

    /** 取消订单：仅待支付订单可取消（释放可收口径占用）；已支付订单需走退款流程，不支持直接取消。 */
    @Transactional
    public ParkingOrderView cancelOrder(Long orderId) {
        adminGuard.requireEnabledAdmin();
        ParkingOrder order = requireOrder(orderId);
        if (order.getStatus() != ParkingOrderStatus.PENDING) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_BAD_STATE);
        }
        order.setStatus(ParkingOrderStatus.CANCELLED);
        order.setPayTime(null);
        return ParkingOrderView.from(orders.save(order));
    }

    /**
     * 退款：已支付或部分退款订单可继续退；{@code amountYuan} 缺省则按剩余可退全额退款。
     * 线上微信/支付宝（非 mock）先调渠道退款，成功后再回冲关联流水累计已支付。
     * 可多次部分退款，直至剩余为 0 变为已全额退款。现金/联调单仅本地入账。
     */
    @Transactional
    public ParkingOrderView refundOrder(Long orderId, RefundParkingOrderRequest request) {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        ParkingOrder order = requireOrder(orderId);
        if (order.getStatus() != ParkingOrderStatus.PAID
                && order.getStatus() != ParkingOrderStatus.PARTIAL_REFUND) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_BAD_STATE);
        }
        BigDecimal remaining = order.refundableYuan();
        BigDecimal amount = request == null || request.amountYuan() == null
                ? remaining
                : request.amountYuan().setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_REFUND_INVALID_AMOUNT);
        }
        if (amount.compareTo(remaining) > 0) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_REFUND_EXCEEDS);
        }
        String reason = null;
        if (request != null && StringUtils.hasText(request.reason())) {
            reason = request.reason().trim();
            if (reason.length() > 200) {
                reason = reason.substring(0, 200);
            }
            order.setRefundReason(reason);
        }

        String refundNo = nextRefundNo();
        refundViaChannelIfNeeded(order, amount, refundNo, reason);

        order.setRefundedYuan(order.refundedOrZero().add(amount));
        order.setRefundTime(SiteZoneTimes.nowUtc());
        BigDecimal remainingAfter = order.getAmountYuan().subtract(order.refundedOrZero());
        if (remainingAfter.signum() < 0) {
            remainingAfter = BigDecimal.ZERO;
        }
        if (remainingAfter.signum() <= 0) {
            order.setStatus(ParkingOrderStatus.REFUNDED);
        } else {
            order.setStatus(ParkingOrderStatus.PARTIAL_REFUND);
        }
        orders.save(order);
        ParkingOrderRefund refund = refunds.save(
                buildRefundRecord(order, operator, refundNo, amount, remainingAfter, reason));
        payRecordService.recordRefund(order, refund, operator);
        sessionService.applyOrderRefund(order.getSessionId(), amount);
        return ParkingOrderView.from(order);
    }

    /**
     * 线上真实支付：按缴款单渠道发起退款；现金单（无 paymentNo）与 mock 单跳过渠道。
     */
    private void refundViaChannelIfNeeded(ParkingOrder order, BigDecimal amount,
                                          String refundNo, String reason) {
        if (!StringUtils.hasText(order.getPaymentNo())) {
            return;
        }
        PaymentOrder payment = payments.findByPayNo(order.getPaymentNo().trim())
                .orElseThrow(() -> new BizException(400, MessageKeys.PAYMENT_NOT_FOUND));
        if (payment.getStatus() != PaymentOrderStatus.PAID) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_REFUND_CHANNEL_UNSUPPORTED);
        }
        if (payment.isMock()) {
            return;
        }
        if (payment.getMethod() == PaymentMethod.WECHAT_PAY) {
            var runtime = weChatConfig.loadJsapiRuntime();
            if (!runtime.ready()) {
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_NOT_CONFIGURED);
            }
            weChatRefund.refund(
                    runtime,
                    payment.getPayNo(),
                    refundNo,
                    amount,
                    payment.getAmountYuan(),
                    reason);
            return;
        }
        if (payment.getMethod() == PaymentMethod.ALIPAY_PAY) {
            var runtime = alipayConfig.loadRuntime();
            if (!runtime.readyToPay()) {
                throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_NOT_CONFIGURED);
            }
            alipayRefund.refund(
                    runtime,
                    payment.getPayNo(),
                    payment.getTransactionId(),
                    refundNo,
                    amount,
                    reason);
            return;
        }
        throw new BizException(400, MessageKeys.PARKING_ORDER_REFUND_CHANNEL_UNSUPPORTED);
    }

    private ParkingOrderRefund buildRefundRecord(ParkingOrder order, UserAccount operator,
                                                 String refundNo, BigDecimal amount,
                                                 BigDecimal remainingAfter, String reason) {
        ParkingOrderRefund record = new ParkingOrderRefund();
        record.setRefundNo(refundNo);
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setSessionId(order.getSessionId());
        record.setLotId(order.getLotId());
        record.setLotName(order.getLotName());
        record.setPlateNumber(order.getPlateNumber());
        record.setPlateColor(order.getPlateColor());
        record.setAmountYuan(amount);
        record.setRefundedAfterYuan(order.refundedOrZero());
        record.setRemainingAfterYuan(remainingAfter);
        record.setRefundType(remainingAfter.signum() <= 0
                ? ParkingRefundType.FULL
                : ParkingRefundType.PARTIAL);
        record.setReason(reason);
        record.setOperatorId(operator.getId());
        record.setOperatorUsername(operator.getUsername());
        record.setOperatorNickname(operator.getNickname());
        return record;
    }

    /**
     * 本单对应的缴费流水：线上缴款按流水拆单时返回同一缴款单下的全部停车订单；
     * 管理端人工下单则仅返回本单自身。
     */
    @Transactional(readOnly = true)
    public List<ParkingOrderView> listPaymentSessions(Long orderId) {
        adminGuard.requireEnabledAdmin();
        ParkingOrder order = requireOrder(orderId);
        if (!StringUtils.hasText(order.getPaymentNo())) {
            return List.of(ParkingOrderView.from(order));
        }
        return orders.findByPaymentNoOrderByIdAsc(order.getPaymentNo()).stream()
                .map(ParkingOrderView::from)
                .toList();
    }

    private ParkingOrder requireOrder(Long orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private Specification<ParkingOrder> buildSpec(Long sessionId, Long lotId, String keyword,
                                                  ParkingOrderStatus status,
                                                  LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sessionId != null) {
                predicates.add(cb.equal(root.get("sessionId"), sessionId));
            }
            if (lotId != null) {
                predicates.add(cb.equal(root.get("lotId"), lotId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNo")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("paymentNo"), "")), like),
                        cb.like(cb.lower(root.get("plateNumber")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("lotName"), "")), like)));
            }
            if (startDate != null || endDate != null) {
                ZoneId zone = siteZoneProvider.currentZone();
                if (startDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("entryTime"),
                            SiteZoneTimes.toUtcAnchor(LocalDateTime.of(startDate, LocalTime.MIN), zone)));
                }
                if (endDate != null) {
                    predicates.add(cb.lessThan(root.get("entryTime"),
                            SiteZoneTimes.toUtcAnchor(LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN), zone)));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 生成唯一业务订单号：UTC 时间戳 + 随机数，对外展示/对账用。 */
    private static String nextOrderNo() {
        return nextBizNo("PO");
    }

    /** 生成唯一退款单号。 */
    private static String nextRefundNo() {
        return nextBizNo("RF");
    }

    private static String nextBizNo(String prefix) {
        String stamp = SiteZoneTimes.nowUtc().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(9000) + 1000;
        return prefix + stamp + random;
    }
}
