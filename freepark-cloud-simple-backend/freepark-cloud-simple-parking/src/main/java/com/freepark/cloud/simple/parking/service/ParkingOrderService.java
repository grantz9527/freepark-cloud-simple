package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.ParkingOrderView;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.ParkingOrderStatus;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSessionRepository;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
 * 或 CANCELLED（取消，释放占用）。
 */
@Service
public class ParkingOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ParkingOrderRepository orders;
    private final ParkingSessionRepository sessions;
    private final ParkingSessionService sessionService;
    private final AdminGuard adminGuard;
    private final SiteZoneProvider siteZoneProvider;

    public ParkingOrderService(ParkingOrderRepository orders,
                               ParkingSessionRepository sessions,
                               ParkingSessionService sessionService,
                               AdminGuard adminGuard,
                               SiteZoneProvider siteZoneProvider) {
        this.orders = orders;
        this.sessions = sessions;
        this.sessionService = sessionService;
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
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo(nextOrderNo());
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
        return ParkingOrderView.from(orders.save(order));
    }

    /** 登记收款：待支付订单 → 已支付，并把订单金额累加到关联流水「累计已支付」，支付状态随之自动推导。 */
    @Transactional
    public ParkingOrderView registerPayment(Long orderId) {
        adminGuard.requireEnabledAdmin();
        ParkingOrder order = requireOrder(orderId);
        if (order.getStatus() != ParkingOrderStatus.PENDING) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_BAD_STATE);
        }
        order.setStatus(ParkingOrderStatus.PAID);
        order.setPayTime(SiteZoneTimes.nowUtc());
        orders.save(order);
        sessionService.applyOrderReceivable(order.getSessionId(), order.getAmountYuan());
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
        String stamp = SiteZoneTimes.nowUtc().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(9000) + 1000;
        return "PO" + stamp + random;
    }
}
