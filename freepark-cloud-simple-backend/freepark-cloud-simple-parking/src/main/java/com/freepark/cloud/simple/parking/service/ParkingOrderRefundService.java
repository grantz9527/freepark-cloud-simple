package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.ParkingOrderRefundView;
import com.freepark.cloud.simple.parking.entity.ParkingOrderRefund;
import com.freepark.cloud.simple.parking.entity.ParkingRefundType;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRefundRepository;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 停车订单退款记录查询。
 */
@Service
public class ParkingOrderRefundService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ParkingOrderRefundRepository refunds;
    private final ParkingOrderRepository orders;
    private final AdminGuard adminGuard;
    private final SiteZoneProvider siteZoneProvider;

    public ParkingOrderRefundService(ParkingOrderRefundRepository refunds,
                                     ParkingOrderRepository orders,
                                     AdminGuard adminGuard,
                                     SiteZoneProvider siteZoneProvider) {
        this.refunds = refunds;
        this.orders = orders;
        this.adminGuard = adminGuard;
        this.siteZoneProvider = siteZoneProvider;
    }

    /** 指定订单的退款记录，按退款时间倒序。 */
    @Transactional(readOnly = true)
    public List<ParkingOrderRefundView> listByOrder(Long orderId) {
        adminGuard.requireEnabledAdmin();
        if (orderId == null || orders.findById(orderId).isEmpty()) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        return refunds.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(ParkingOrderRefundView::from)
                .toList();
    }

    /** 退款记录分页：按车场/关键字（退款单号、订单号、车牌、车场）/类型/退款日期筛选。 */
    @Transactional(readOnly = true)
    public PageResult<ParkingOrderRefundView> listRefunds(Long orderId, Long lotId, String keyword,
                                                          ParkingRefundType refundType,
                                                          LocalDate startDate, LocalDate endDate,
                                                          int page, int size) {
        adminGuard.requireEnabledAdmin();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<ParkingOrderRefund> result = refunds.findAll(
                buildSpec(orderId, lotId, keyword, refundType, startDate, endDate),
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<ParkingOrderRefundView> items = result.getContent().stream()
                .map(ParkingOrderRefundView::from)
                .toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    private Specification<ParkingOrderRefund> buildSpec(Long orderId, Long lotId, String keyword,
                                                        ParkingRefundType refundType,
                                                        LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (orderId != null) {
                predicates.add(cb.equal(root.get("orderId"), orderId));
            }
            if (lotId != null) {
                predicates.add(cb.equal(root.get("lotId"), lotId));
            }
            if (refundType != null) {
                predicates.add(cb.equal(root.get("refundType"), refundType));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("refundNo")), like),
                        cb.like(cb.lower(root.get("orderNo")), like),
                        cb.like(cb.lower(root.get("plateNumber")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("lotName"), "")), like)));
            }
            if (startDate != null || endDate != null) {
                ZoneId zone = siteZoneProvider.currentZone();
                if (startDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                            SiteZoneTimes.toUtcAnchor(LocalDateTime.of(startDate, LocalTime.MIN), zone)));
                }
                if (endDate != null) {
                    predicates.add(cb.lessThan(root.get("createdAt"),
                            SiteZoneTimes.toUtcAnchor(LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN), zone)));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
