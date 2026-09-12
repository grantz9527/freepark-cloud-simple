package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.PayGlobalDailyStatsView;
import com.freepark.cloud.simple.parking.dto.PayLotDailyCsvExport;
import com.freepark.cloud.simple.parking.dto.PayLotDailyStatsView;
import com.freepark.cloud.simple.parking.dto.PayRecordView;
import com.freepark.cloud.simple.parking.entity.PayRecordKind;
import com.freepark.cloud.simple.parking.entity.PayRecordPlatform;
import com.freepark.cloud.simple.parking.entity.PayRecordStatus;
import com.freepark.cloud.simple.parking.service.PayRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 支付管理 - 支付记录：各支付平台的支付请求与退款请求。
 */
@RestController
@RequestMapping("/api/pay-records")
public class PayRecordController {

    private final PayRecordService payRecordService;

    public PayRecordController(PayRecordService payRecordService) {
        this.payRecordService = payRecordService;
    }

    @GetMapping
    public ApiResult<PageResult<PayRecordView>> list(
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PayRecordKind kind,
            @RequestParam(required = false) PayRecordPlatform platform,
            @RequestParam(required = false) PayRecordStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(payRecordService.listRecords(
                lotId, keyword, kind, platform, status, startDate, endDate, page, size));
    }

    /** 车场按日收费：成功支付/退款按站点自然日与车场汇总。 */
    @GetMapping("/lot-daily-stats")
    public ApiResult<PayLotDailyStatsView> lotDailyStats(
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) PayRecordPlatform platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(payRecordService.listLotDailyStats(
                lotId, platform, startDate, endDate, page, size));
    }

    /** 全局按日收费：成功支付/退款按站点自然日汇总（全车场）。 */
    @GetMapping("/global-daily-stats")
    public ApiResult<PayGlobalDailyStatsView> globalDailyStats(
            @RequestParam(required = false) PayRecordPlatform platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(payRecordService.listGlobalDailyStats(platform, startDate, endDate, page, size));
    }

    /** 导出全局按日收费 CSV；不要求车场。 */
    @GetMapping("/global-daily-stats/export")
    public ResponseEntity<byte[]> exportGlobalDailyStats(
            @RequestParam(required = false) PayRecordPlatform platform,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PayLotDailyCsvExport csv = payRecordService.exportGlobalDailyStats(platform, startDate, endDate);
        String encoded = URLEncoder.encode(csv.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        String ascii = csv.filename().replaceAll("[^A-Za-z0-9._-]", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.content());
    }

    /** 导出指定车场的按日收费 CSV；必须传入车场。 */
    @GetMapping("/lot-daily-stats/export")
    public ResponseEntity<byte[]> exportLotDailyStats(
            @RequestParam Long lotId,
            @RequestParam(required = false) PayRecordPlatform platform,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PayLotDailyCsvExport csv = payRecordService.exportLotDailyStats(lotId, platform, startDate, endDate);
        String encoded = URLEncoder.encode(csv.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        String ascii = csv.filename().replaceAll("[^A-Za-z0-9._-]", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.content());
    }
}
