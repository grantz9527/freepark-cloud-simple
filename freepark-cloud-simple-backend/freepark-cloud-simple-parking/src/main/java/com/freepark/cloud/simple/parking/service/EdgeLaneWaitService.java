package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.parking.dto.EdgeArrearsQuoteRequest;
import com.freepark.cloud.simple.parking.dto.EdgeLaneWaitRequest;
import com.freepark.cloud.simple.parking.dto.EdgeLaneWaitView;
import com.freepark.cloud.simple.parking.entity.InterceptRuleType;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.repository.ParkingLaneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 通道「欠费拦截等待缴费」快照。
 *
 * <p>欠费拦截不上报离场流水：识别路径走算费接口并带上 {@code laneCode} 时，
 * 金额 &gt; 0 记下等待，金额为 0 或新识别覆盖则清空。缴费开闸只认仍有效的这条记录。</p>
 */
@Service
public class EdgeLaneWaitService {

    /** 欠费拦截后允许缴费开闸的时效（分钟）。 */
    public static final int WAIT_TTL_MINUTES = 15;

    public static final String REASON_CLEAR = "CLEAR";

    private final ParkingLaneRepository lanes;

    public EdgeLaneWaitService(ParkingLaneRepository lanes) {
        this.lanes = lanes;
    }

    @Transactional
    public EdgeLaneWaitView report(EdgeLaneWaitRequest request) {
        if (request == null || !StringUtils.hasText(request.laneCode())) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingLane lane = lanes.findByCodeIgnoreCase(request.laneCode().trim())
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        ParkingLot lot = lane.getLot();
        if (StringUtils.hasText(request.lotCode())
                && lot != null
                && !request.lotCode().trim().equalsIgnoreCase(lot.getCode())) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        if (StringUtils.hasText(request.edgeCode())
                && lot != null
                && StringUtils.hasText(lot.getEdgeNodeCode())
                && !request.edgeCode().trim().equals(lot.getEdgeNodeCode().trim())) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }

        String reason = request.reason() == null ? "" : request.reason().trim().toUpperCase();
        if (!InterceptRuleType.ARREARS.name().equals(reason)) {
            lane.clearWait();
            lanes.save(lane);
            return EdgeLaneWaitView.cleared(lane.getCode());
        }

        String plate = request.plateNumber() == null ? "" : request.plateNumber().trim().toUpperCase();
        if (!StringUtils.hasText(plate)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        LocalDateTime recognizedAt = parseRecognizedAt(request.recognizedAt());
        String recognitionId = blankToNull(request.recognitionId());
        if (recognitionId != null && recognitionId.length() > 64) {
            recognitionId = recognitionId.substring(0, 64);
        }
        lane.markArrearsWait(plate, parseColor(request.plateColor()), recognizedAt, recognitionId);
        lanes.save(lane);
        return EdgeLaneWaitView.waiting(lane.getCode(), plate);
    }

    /**
     * 识别算费结果回写通道等待。无 {@code laneCode}（探活/试算）时不改等待。
     * 欠费则覆盖为该车牌等待；未欠费则清空，表示新识别已覆盖上次拦截。
     */
    @Transactional
    public void rememberFromQuote(EdgeArrearsQuoteRequest request, BigDecimal amount) {
        if (request == null || !StringUtils.hasText(request.laneCode())) {
            return;
        }
        ParkingLane lane = lanes.findByCodeIgnoreCase(request.laneCode().trim()).orElse(null);
        if (lane == null || !lane.isEnabled()) {
            return;
        }
        ParkingLot lot = lane.getLot();
        if (StringUtils.hasText(request.lotCode())
                && lot != null
                && StringUtils.hasText(lot.getCode())
                && !request.lotCode().trim().equalsIgnoreCase(lot.getCode())) {
            return;
        }
        if (StringUtils.hasText(request.edgeCode())
                && lot != null
                && StringUtils.hasText(lot.getEdgeNodeCode())
                && !request.edgeCode().trim().equals(lot.getEdgeNodeCode().trim())) {
            return;
        }
        if (amount == null || amount.signum() <= 0) {
            if (lane.getWaitReason() != null) {
                lane.clearWait();
                lanes.save(lane);
            }
            return;
        }
        String plate = request.plateNumber() == null ? "" : request.plateNumber().trim().toUpperCase();
        if (!StringUtils.hasText(plate)) {
            return;
        }
        String recognitionId = blankToNull(request.recognitionId());
        if (recognitionId != null && recognitionId.length() > 64) {
            recognitionId = recognitionId.substring(0, 64);
        }
        lane.markArrearsWait(plate, parseColor(request.plateColor()), SiteZoneTimes.nowUtc(), recognitionId);
        lanes.save(lane);
    }

    /**
     * 缴费成功后可开闸的通道：该通道最新识别是这辆车的欠费拦截，且仍在 15 分钟内。
     */
    @Transactional(readOnly = true)
    public List<ParkingLane> findPayableWaitLanes(String plateNumber, PlateColor plateColor) {
        if (!StringUtils.hasText(plateNumber)) {
            return List.of();
        }
        String plate = plateNumber.trim().toUpperCase();
        LocalDateTime since = SiteZoneTimes.nowUtc().minusMinutes(WAIT_TTL_MINUTES);
        List<ParkingLane> matched = new ArrayList<>();
        for (ParkingLane lane : lanes.findByEnabledTrueAndWaitReasonAndWaitPlateNumberIgnoreCase(
                InterceptRuleType.ARREARS, plate)) {
            if (lane.getWaitRecognizedAt() == null || lane.getWaitRecognizedAt().isBefore(since)) {
                continue;
            }
            if (plateColor != null
                    && lane.getWaitPlateColor() != null
                    && plateColor != lane.getWaitPlateColor()) {
                continue;
            }
            ParkingLot lot = lane.getLot();
            if (lot == null || !StringUtils.hasText(lot.getEdgeNodeCode()) || !StringUtils.hasText(lot.getCode())) {
                continue;
            }
            matched.add(lane);
        }
        return matched;
    }

    @Transactional
    public void clearWait(Long laneId) {
        if (laneId == null) {
            return;
        }
        lanes.findById(laneId).ifPresent(lane -> {
            lane.clearWait();
            lanes.save(lane);
        });
    }

    private static LocalDateTime parseRecognizedAt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return SiteZoneTimes.nowUtc();
        }
        try {
            Instant instant = Instant.parse(raw.trim());
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (RuntimeException ignored) {
            return SiteZoneTimes.nowUtc();
        }
    }

    private static PlateColor parseColor(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return PlateColor.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
