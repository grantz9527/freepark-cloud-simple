package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.parking.dto.PlateFeeQuoteView;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.service.ParkingSessionService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端（用户端网页）公开查询接口：面向车主，无需登录。
 * 该命名空间在 WebAuthConfig 中整体放行（免 JWT），仅暴露按车牌的费用查询，
 * 不含任何车场/流水管理能力。
 */
@RestController
@RequestMapping("/api/public")
public class PublicPlateFeeController {

    private final ParkingSessionService parkingSessionService;

    public PublicPlateFeeController(ParkingSessionService parkingSessionService) {
        this.parkingSessionService = parkingSessionService;
    }

    /**
     * 按车牌查询当前费用信息（在停估算 + 历史未结明细与合计）。
     * 可选 {@code plateColor}（枚举名：BLUE/GREEN/YELLOW/YELLOW_GREEN/BLACK/WHITE/OTHER）：
     * 同一车牌存在不同颜色记录时，传值可仅查询该颜色的费用，避免混淆。
     */
    @GetMapping("/plate-fee")
    public PlateFeeQuoteView plateFee(@RequestParam("plateNumber") String plateNumber,
                                      @RequestParam(value = "plateColor", required = false) String plateColor) {
        return parkingSessionService.queryPublicPlateFee(plateNumber, resolveColor(plateColor));
    }

    private static PlateColor resolveColor(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return PlateColor.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
    }
}
