package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.PayGlobalDailyStatRow;
import com.freepark.cloud.simple.parking.dto.PayGlobalDailyStatsView;
import com.freepark.cloud.simple.parking.dto.PayLotDailyCsvExport;
import com.freepark.cloud.simple.parking.dto.PayLotDailyStatRow;
import com.freepark.cloud.simple.parking.dto.PayLotDailyStatsView;
import com.freepark.cloud.simple.parking.dto.PayRecordLotView;
import com.freepark.cloud.simple.parking.dto.PayRecordView;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.ParkingOrderRefund;
import com.freepark.cloud.simple.parking.entity.PayRecord;
import com.freepark.cloud.simple.parking.entity.PayRecordKind;
import com.freepark.cloud.simple.parking.entity.PayRecordLot;
import com.freepark.cloud.simple.parking.entity.PayRecordPlatform;
import com.freepark.cloud.simple.parking.entity.PayRecordStatus;
import com.freepark.cloud.simple.parking.entity.PaymentOrder;
import com.freepark.cloud.simple.parking.repository.PayRecordLotRepository;
import com.freepark.cloud.simple.parking.repository.PayRecordRepository;
import com.freepark.cloud.simple.parking.repository.PaymentOrderRepository;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付平台流水：每次支付请求、每次退款请求落一行，并按车场拆开金额。
 */
@Service
public class PayRecordService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PayRecordRepository records;
    private final PayRecordLotRepository recordLots;
    private final PaymentOrderRepository payments;
    private final AdminGuard adminGuard;
    private final SiteZoneProvider siteZoneProvider;

    public PayRecordService(PayRecordRepository records,
                            PayRecordLotRepository recordLots,
                            PaymentOrderRepository payments,
                            AdminGuard adminGuard,
                            SiteZoneProvider siteZoneProvider) {
        this.records = records;
        this.recordLots = recordLots;
        this.payments = payments;
        this.adminGuard = adminGuard;
        this.siteZoneProvider = siteZoneProvider;
    }

    /** 线上缴款下单：记一笔待确认的支付请求，车场金额按拆出的停车订单汇总。 */
    @Transactional
    public void recordOnlinePay(PaymentOrder payment, List<ParkingOrder> orders) {
        if (payment == null || !StringUtils.hasText(payment.getPayNo())) {
            return;
        }
        if (records.findByKindAndRelatedPayNo(PayRecordKind.PAY, payment.getPayNo()).isPresent()) {
            return;
        }
        PayRecord record = new PayRecord();
        record.setRecordNo(payment.getPayNo());
        record.setKind(PayRecordKind.PAY);
        record.setPlatform(PayRecordPlatform.from(payment.getMethod()));
        record.setStatus(PayRecordStatus.PENDING);
        record.setAmountYuan(payment.getAmountYuan());
        record.setPlateNumber(payment.getPlateNumber());
        record.setPlateColor(payment.getPlateColor());
        record.setRelatedPayNo(payment.getPayNo());
        record.setMock(payment.isMock());
        records.save(record);
        saveLots(record.getId(), orders);
    }

    /** 渠道确认支付成功（或管理端把线上缴款记为已收）。 */
    @Transactional
    public void markPaySuccess(String payNo, String transactionId, LocalDateTime successTime) {
        records.findByKindAndRelatedPayNo(PayRecordKind.PAY, payNo).ifPresent(record -> {
            if (record.getStatus() != PayRecordStatus.PENDING) {
                return;
            }
            record.setStatus(PayRecordStatus.SUCCESS);
            record.setTransactionId(transactionId);
            record.setSuccessTime(successTime == null ? SiteZoneTimes.nowUtc() : successTime);
            records.save(record);
        });
    }

    /** 支付关闭：用户取消、渠道失败或重新下单。 */
    @Transactional
    public void markPayClosed(String payNo) {
        records.findByKindAndRelatedPayNo(PayRecordKind.PAY, payNo).ifPresent(record -> {
            if (record.getStatus() != PayRecordStatus.PENDING) {
                return;
            }
            record.setStatus(PayRecordStatus.CLOSED);
            records.save(record);
        });
    }

    /** 管理端现金/人工登记收款：当场成功，单车场。 */
    @Transactional
    public void recordCashPay(ParkingOrder order, UserAccount operator) {
        if (order == null) {
            return;
        }
        PayRecord record = new PayRecord();
        record.setRecordNo(nextCashNo());
        record.setKind(PayRecordKind.PAY);
        record.setPlatform(PayRecordPlatform.CASH);
        record.setStatus(PayRecordStatus.SUCCESS);
        record.setAmountYuan(order.getAmountYuan());
        record.setPlateNumber(order.getPlateNumber());
        record.setPlateColor(order.getPlateColor());
        record.setRelatedOrderNo(order.getOrderNo());
        record.setMock(false);
        record.setSuccessTime(order.getPayTime() == null ? SiteZoneTimes.nowUtc() : order.getPayTime());
        applyOperator(record, operator);
        records.save(record);
        saveLots(record.getId(), List.of(order));
    }

    /** 退款请求：按原支付平台记账，金额记到对应车场。 */
    @Transactional
    public void recordRefund(ParkingOrder order, ParkingOrderRefund refund, UserAccount operator) {
        if (order == null || refund == null) {
            return;
        }
        PayRecord record = new PayRecord();
        record.setRecordNo(refund.getRefundNo());
        record.setKind(PayRecordKind.REFUND);
        record.setPlatform(resolvePlatform(order));
        record.setStatus(PayRecordStatus.SUCCESS);
        record.setAmountYuan(refund.getAmountYuan());
        record.setPlateNumber(order.getPlateNumber());
        record.setPlateColor(order.getPlateColor());
        record.setRelatedPayNo(order.getPaymentNo());
        record.setRelatedOrderNo(order.getOrderNo());
        record.setRelatedRefundNo(refund.getRefundNo());
        record.setMock(false);
        record.setSuccessTime(refund.getCreatedAt() == null ? SiteZoneTimes.nowUtc() : refund.getCreatedAt());
        applyOperator(record, operator);
        records.save(record);
        PayRecordLot lot = new PayRecordLot();
        lot.setRecordId(record.getId());
        lot.setLotId(order.getLotId());
        lot.setLotName(order.getLotName());
        lot.setAmountYuan(refund.getAmountYuan());
        recordLots.save(lot);
    }

    /** 支付记录分页：平台/类型/状态/车场/关键字/日期。 */
    @Transactional(readOnly = true)
    public PageResult<PayRecordView> listRecords(Long lotId, String keyword,
                                                 PayRecordKind kind, PayRecordPlatform platform,
                                                 PayRecordStatus status,
                                                 LocalDate startDate, LocalDate endDate,
                                                 int page, int size) {
        adminGuard.requireEnabledAdmin();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<PayRecord> result = records.findAll(
                buildSpec(lotId, keyword, kind, platform, status, startDate, endDate),
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PayRecord> content = result.getContent();
        Map<Long, List<PayRecordLotView>> lotsByRecord = loadLots(content.stream().map(PayRecord::getId).toList());
        List<PayRecordView> items = content.stream()
                .map(row -> PayRecordView.from(row, lotsByRecord.getOrDefault(row.getId(), List.of())))
                .toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    /**
     * 车场按日收费：仅统计成功的支付/退款，按站点时区自然日 × 车场汇总。
     * 合计为筛选范围内全部行（不受当前页限制）。
     */
    @Transactional(readOnly = true)
    public PayLotDailyStatsView listLotDailyStats(Long lotId, PayRecordPlatform platform,
                                                  LocalDate startDate, LocalDate endDate,
                                                  int page, int size) {
        adminGuard.requireEnabledAdmin();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LotDailyStatsData data = collectLotDailyStats(lotId, platform, startDate, endDate);
        int from = Math.min((safePage - 1) * safeSize, data.rows.size());
        int to = Math.min(from + safeSize, data.rows.size());
        return new PayLotDailyStatsView(
                data.rows.subList(from, to),
                data.rows.size(),
                safePage,
                safeSize,
                data.payYuan,
                data.refundYuan,
                data.netYuan());
    }

    /**
     * 全局按日收费：全车场成功支付/退款按站点时区自然日汇总。
     * 合计为筛选范围内全部行（不受当前页限制）。
     */
    @Transactional(readOnly = true)
    public PayGlobalDailyStatsView listGlobalDailyStats(PayRecordPlatform platform,
                                                        LocalDate startDate, LocalDate endDate,
                                                        int page, int size) {
        adminGuard.requireEnabledAdmin();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        GlobalDailyStatsData data = collectGlobalDailyStats(platform, startDate, endDate);
        int from = Math.min((safePage - 1) * safeSize, data.rows.size());
        int to = Math.min(from + safeSize, data.rows.size());
        return new PayGlobalDailyStatsView(
                data.rows.subList(from, to),
                data.rows.size(),
                safePage,
                safeSize,
                data.payYuan,
                data.refundYuan,
                data.netYuan());
    }

    /** 导出全局按日统计 CSV（Excel 可打开）。 */
    @Transactional(readOnly = true)
    public PayLotDailyCsvExport exportGlobalDailyStats(PayRecordPlatform platform,
                                                       LocalDate startDate, LocalDate endDate) {
        adminGuard.requireEnabledAdmin();
        if (startDate == null || endDate == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        GlobalDailyStatsData data = collectGlobalDailyStats(platform, startDate, endDate);
        boolean english = LocaleContextHolder.getLocale().getLanguage().startsWith("en");
        String[] headers = english
                ? new String[] {"Date", "Paid", "WeChat Pay", "Alipay", "Cash", "Refunded", "Net"}
                : new String[] {"日期", "收款", "微信支付", "支付宝", "现金收款", "退款", "实收"};
        String totalLabel = english ? "Total" : "合计";
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append(csvLine(headers));
        for (PayGlobalDailyStatRow row : data.rows) {
            csv.append(csvLine(new String[] {
                    String.valueOf(row.statDate()),
                    moneyCsv(row.payYuan()),
                    moneyCsv(row.wechatYuan()),
                    moneyCsv(row.alipayYuan()),
                    moneyCsv(row.cashYuan()),
                    moneyCsv(row.refundYuan()),
                    moneyCsv(row.netYuan())
            }));
        }
        csv.append(csvLine(new String[] {
                totalLabel,
                moneyCsv(data.payYuan),
                "",
                "",
                "",
                moneyCsv(data.refundYuan),
                moneyCsv(data.netYuan())
        }));
        String range = startDate + "_" + endDate;
        String filename = (english ? "global-stats_" : "全局统计_") + range + ".csv";
        return new PayLotDailyCsvExport(csv.toString().getBytes(StandardCharsets.UTF_8), filename);
    }

    /** 导出指定车场的按日统计 CSV；未选车场时拒绝。 */
    @Transactional(readOnly = true)
    public PayLotDailyCsvExport exportLotDailyStats(Long lotId, PayRecordPlatform platform,
                                                    LocalDate startDate, LocalDate endDate) {
        adminGuard.requireEnabledAdmin();
        if (lotId == null) {
            throw new BizException(400, MessageKeys.PAY_LOT_STATS_LOT_REQUIRED);
        }
        if (startDate == null || endDate == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        LotDailyStatsData data = collectLotDailyStats(lotId, platform, startDate, endDate);
        boolean english = LocaleContextHolder.getLocale().getLanguage().startsWith("en");
        String lotName = data.rows.stream()
                .map(PayLotDailyStatRow::lotName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("lot-" + lotId);
        String[] headers = english
                ? new String[] {"Date", "Lot", "Paid", "WeChat Pay", "Alipay", "Cash", "Refunded", "Net"}
                : new String[] {"日期", "车场", "收款", "微信支付", "支付宝", "现金收款", "退款", "实收"};
        String totalLabel = english ? "Total" : "合计";
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append(csvLine(headers));
        for (PayLotDailyStatRow row : data.rows) {
            csv.append(csvLine(new String[] {
                    String.valueOf(row.statDate()),
                    row.lotName() == null ? "" : row.lotName(),
                    moneyCsv(row.payYuan()),
                    moneyCsv(row.wechatYuan()),
                    moneyCsv(row.alipayYuan()),
                    moneyCsv(row.cashYuan()),
                    moneyCsv(row.refundYuan()),
                    moneyCsv(row.netYuan())
            }));
        }
        csv.append(csvLine(new String[] {
                totalLabel,
                lotName,
                moneyCsv(data.payYuan),
                "",
                "",
                "",
                moneyCsv(data.refundYuan),
                moneyCsv(data.netYuan())
        }));
        String range = startDate + "_" + endDate;
        String filename = (english ? "lot-stats_" : "车场统计_") + lotName + "_" + range + ".csv";
        return new PayLotDailyCsvExport(csv.toString().getBytes(StandardCharsets.UTF_8), filename);
    }

    private LotDailyStatsData collectLotDailyStats(Long lotId, PayRecordPlatform platform,
                                                   LocalDate startDate, LocalDate endDate) {
        ZoneId zone = siteZoneProvider.currentZone();
        LocalDateTime startUtc = startDate == null
                ? null
                : SiteZoneTimes.toUtcAnchor(LocalDateTime.of(startDate, LocalTime.MIN), zone);
        LocalDateTime endUtc = endDate == null
                ? null
                : SiteZoneTimes.toUtcAnchor(LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN), zone);
        if (startUtc == null || endUtc == null) {
            return new LotDailyStatsData(List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Map<String, DailyLotAgg> grouped = new LinkedHashMap<>();
        for (Object[] row : recordLots.findLotsWithRecord(PayRecordStatus.SUCCESS, startUtc, endUtc)) {
            PayRecordLot lot = (PayRecordLot) row[0];
            PayRecord record = (PayRecord) row[1];
            if (lotId != null && (lot.getLotId() == null || !lotId.equals(lot.getLotId()))) {
                continue;
            }
            if (platform != null && record.getPlatform() != platform) {
                continue;
            }
            LocalDateTime when = record.getSuccessTime() != null ? record.getSuccessTime() : record.getCreatedAt();
            LocalDate statDate = SiteZoneTimes.toSiteWall(when, zone).toLocalDate();
            String key = statDate + "|" + (lot.getLotId() == null ? "none" : lot.getLotId());
            DailyLotAgg agg = grouped.get(key);
            if (agg == null) {
                agg = new DailyLotAgg(statDate, lot.getLotId(), lot.getLotName());
                grouped.put(key, agg);
            } else if (!StringUtils.hasText(agg.lotName) && StringUtils.hasText(lot.getLotName())) {
                agg.lotName = lot.getLotName();
            }
            BigDecimal amount = lot.getAmountYuan() == null ? BigDecimal.ZERO : lot.getAmountYuan();
            if (record.getKind() == PayRecordKind.REFUND) {
                agg.refundYuan = agg.refundYuan.add(amount);
            } else {
                agg.payYuan = agg.payYuan.add(amount);
                if (record.getPlatform() == PayRecordPlatform.ALIPAY_PAY) {
                    agg.alipayYuan = agg.alipayYuan.add(amount);
                } else if (record.getPlatform() == PayRecordPlatform.CASH) {
                    agg.cashYuan = agg.cashYuan.add(amount);
                } else {
                    agg.wechatYuan = agg.wechatYuan.add(amount);
                }
            }
        }
        List<PayLotDailyStatRow> all = grouped.values().stream()
                .map(DailyLotAgg::toRow)
                .sorted(Comparator.comparing(PayLotDailyStatRow::statDate).reversed()
                        .thenComparing(row -> row.lotName() == null ? "" : row.lotName()))
                .toList();
        BigDecimal payTotal = BigDecimal.ZERO;
        BigDecimal refundTotal = BigDecimal.ZERO;
        for (PayLotDailyStatRow row : all) {
            payTotal = payTotal.add(row.payYuan());
            refundTotal = refundTotal.add(row.refundYuan());
        }
        return new LotDailyStatsData(all, payTotal, refundTotal);
    }

    private GlobalDailyStatsData collectGlobalDailyStats(PayRecordPlatform platform,
                                                         LocalDate startDate, LocalDate endDate) {
        LotDailyStatsData lotData = collectLotDailyStats(null, platform, startDate, endDate);
        Map<LocalDate, DailyLotAgg> byDate = new LinkedHashMap<>();
        for (PayLotDailyStatRow row : lotData.rows) {
            DailyLotAgg agg = byDate.get(row.statDate());
            if (agg == null) {
                agg = new DailyLotAgg(row.statDate(), null, null);
                byDate.put(row.statDate(), agg);
            }
            agg.payYuan = agg.payYuan.add(row.payYuan());
            agg.wechatYuan = agg.wechatYuan.add(row.wechatYuan());
            agg.alipayYuan = agg.alipayYuan.add(row.alipayYuan());
            agg.cashYuan = agg.cashYuan.add(row.cashYuan());
            agg.refundYuan = agg.refundYuan.add(row.refundYuan());
        }
        List<PayGlobalDailyStatRow> all = byDate.values().stream()
                .map(agg -> new PayGlobalDailyStatRow(
                        agg.statDate,
                        agg.payYuan,
                        agg.wechatYuan,
                        agg.alipayYuan,
                        agg.cashYuan,
                        agg.refundYuan,
                        agg.payYuan.subtract(agg.refundYuan)))
                .sorted(Comparator.comparing(PayGlobalDailyStatRow::statDate).reversed())
                .toList();
        return new GlobalDailyStatsData(all, lotData.payYuan, lotData.refundYuan);
    }

    private static String csvLine(String[] cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(csvCell(cells[i]));
        }
        return line.append('\n').toString();
    }

    private static String csvCell(String value) {
        String text = value == null ? "" : value;
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private static String moneyCsv(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private Specification<PayRecord> buildSpec(Long lotId, String keyword,
                                               PayRecordKind kind, PayRecordPlatform platform,
                                               PayRecordStatus status,
                                               LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (kind != null) {
                predicates.add(cb.equal(root.get("kind"), kind));
            }
            if (platform != null) {
                predicates.add(cb.equal(root.get("platform"), platform));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (lotId != null && query != null) {
                Subquery<Long> exists = query.subquery(Long.class);
                Root<PayRecordLot> lotRoot = exists.from(PayRecordLot.class);
                exists.select(lotRoot.get("id"));
                exists.where(
                        cb.equal(lotRoot.get("recordId"), root.get("id")),
                        cb.equal(lotRoot.get("lotId"), lotId));
                predicates.add(cb.exists(exists));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                List<Predicate> keywordOr = new ArrayList<>();
                keywordOr.add(cb.like(cb.lower(root.get("recordNo")), like));
                keywordOr.add(cb.like(cb.lower(root.get("plateNumber")), like));
                keywordOr.add(cb.like(cb.lower(cb.coalesce(root.get("relatedPayNo"), "")), like));
                keywordOr.add(cb.like(cb.lower(cb.coalesce(root.get("relatedOrderNo"), "")), like));
                keywordOr.add(cb.like(cb.lower(cb.coalesce(root.get("relatedRefundNo"), "")), like));
                keywordOr.add(cb.like(cb.lower(cb.coalesce(root.get("transactionId"), "")), like));
                if (query != null) {
                    Subquery<Long> lotExists = query.subquery(Long.class);
                    Root<PayRecordLot> lotRoot = lotExists.from(PayRecordLot.class);
                    lotExists.select(lotRoot.get("id"));
                    lotExists.where(
                            cb.equal(lotRoot.get("recordId"), root.get("id")),
                            cb.like(cb.lower(cb.coalesce(lotRoot.get("lotName"), "")), like));
                    keywordOr.add(cb.exists(lotExists));
                }
                predicates.add(cb.or(keywordOr.toArray(new Predicate[0])));
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

    private Map<Long, List<PayRecordLotView>> loadLots(List<Long> recordIds) {
        Map<Long, List<PayRecordLotView>> map = new LinkedHashMap<>();
        if (recordIds == null || recordIds.isEmpty()) {
            return map;
        }
        for (PayRecordLot lot : recordLots.findByRecordIdInOrderByIdAsc(recordIds)) {
            map.computeIfAbsent(lot.getRecordId(), key -> new ArrayList<>()).add(PayRecordLotView.from(lot));
        }
        return map;
    }

    private void saveLots(Long recordId, List<ParkingOrder> orders) {
        if (recordId == null || orders == null || orders.isEmpty()) {
            return;
        }
        Map<String, PayRecordLot> grouped = new LinkedHashMap<>();
        for (ParkingOrder order : orders) {
            if (order == null || order.getAmountYuan() == null) {
                continue;
            }
            String key = (order.getLotId() == null ? "none" : order.getLotId().toString())
                    + "|" + (order.getLotName() == null ? "" : order.getLotName());
            PayRecordLot lot = grouped.get(key);
            if (lot == null) {
                lot = new PayRecordLot();
                lot.setRecordId(recordId);
                lot.setLotId(order.getLotId());
                lot.setLotName(order.getLotName());
                lot.setAmountYuan(order.getAmountYuan());
                grouped.put(key, lot);
            } else {
                lot.setAmountYuan(lot.getAmountYuan().add(order.getAmountYuan()));
            }
        }
        recordLots.saveAll(grouped.values());
    }

    private PayRecordPlatform resolvePlatform(ParkingOrder order) {
        if (!StringUtils.hasText(order.getPaymentNo())) {
            return PayRecordPlatform.CASH;
        }
        return records.findByKindAndRelatedPayNo(PayRecordKind.PAY, order.getPaymentNo())
                .map(PayRecord::getPlatform)
                .or(() -> payments.findByPayNo(order.getPaymentNo()).map(p -> PayRecordPlatform.from(p.getMethod())))
                .orElse(PayRecordPlatform.CASH);
    }

    private static void applyOperator(PayRecord record, UserAccount operator) {
        if (operator == null) {
            return;
        }
        record.setOperatorId(operator.getId());
        record.setOperatorUsername(operator.getUsername());
        record.setOperatorNickname(operator.getNickname());
    }

    private static String nextCashNo() {
        String stamp = SiteZoneTimes.nowUtc().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(9000) + 1000;
        return "CS" + stamp + random;
    }

    private static final class GlobalDailyStatsData {
        private final List<PayGlobalDailyStatRow> rows;
        private final BigDecimal payYuan;
        private final BigDecimal refundYuan;

        private GlobalDailyStatsData(List<PayGlobalDailyStatRow> rows, BigDecimal payYuan, BigDecimal refundYuan) {
            this.rows = rows;
            this.payYuan = payYuan;
            this.refundYuan = refundYuan;
        }

        private BigDecimal netYuan() {
            return payYuan.subtract(refundYuan);
        }
    }

    private static final class LotDailyStatsData {
        private final List<PayLotDailyStatRow> rows;
        private final BigDecimal payYuan;
        private final BigDecimal refundYuan;

        private LotDailyStatsData(List<PayLotDailyStatRow> rows, BigDecimal payYuan, BigDecimal refundYuan) {
            this.rows = rows;
            this.payYuan = payYuan;
            this.refundYuan = refundYuan;
        }

        private BigDecimal netYuan() {
            return payYuan.subtract(refundYuan);
        }
    }

    private static final class DailyLotAgg {
        private final LocalDate statDate;
        private final Long lotId;
        private String lotName;
        private BigDecimal payYuan = BigDecimal.ZERO;
        private BigDecimal wechatYuan = BigDecimal.ZERO;
        private BigDecimal alipayYuan = BigDecimal.ZERO;
        private BigDecimal cashYuan = BigDecimal.ZERO;
        private BigDecimal refundYuan = BigDecimal.ZERO;

        private DailyLotAgg(LocalDate statDate, Long lotId, String lotName) {
            this.statDate = statDate;
            this.lotId = lotId;
            this.lotName = lotName;
        }

        private PayLotDailyStatRow toRow() {
            return new PayLotDailyStatRow(
                    statDate,
                    lotId,
                    lotName,
                    payYuan,
                    wechatYuan,
                    alipayYuan,
                    cashYuan,
                    refundYuan,
                    payYuan.subtract(refundYuan));
        }
    }
}
