package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.parking.dto.AccessDecisionRequest;
import com.freepark.cloud.simple.parking.dto.AccessDecisionView;
import com.freepark.cloud.simple.parking.entity.AccessDirection;
import com.freepark.cloud.simple.parking.entity.AccessJudgmentRuleType;
import com.freepark.cloud.simple.parking.entity.LotType;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.PatternAllowlist;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.repository.BlacklistVehicleRepository;
import com.freepark.cloud.simple.parking.repository.InternalVehicleRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLaneRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.PatternAllowlistRepository;
import com.freepark.cloud.simple.parking.repository.WhitelistVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 通行判定引擎：按车场配置的判定顺序（黑名单/白名单/号段放行）与名单数据决定放行或拦截。
 *
 * <p>判定顺序：
 * <ol>
 *   <li>按车场配置的判定顺序执行，首个命中的规则直接决定结果。</li>
 *   <li>内部车场入场要求车牌已登记为内部车辆。</li>
 *   <li>通道（调用方）配置的拦截车牌颜色命中则拦截。</li>
 *   <li>出场无在场流水时仍放行，仅在 remark 中标记。</li>
 * </ol>
 */
@Service
public class AccessDecisionService {

    private final ParkingLotRepository lots;
    private final ParkingLaneRepository lanes;
    private final InternalVehicleRepository internalVehicles;
    private final WhitelistVehicleRepository whitelistVehicles;
    private final BlacklistVehicleRepository blacklistVehicles;
    private final PatternAllowlistRepository patternAllowlist;

    public AccessDecisionService(ParkingLotRepository lots,
                                 ParkingLaneRepository lanes,
                                 InternalVehicleRepository internalVehicles,
                                 WhitelistVehicleRepository whitelistVehicles,
                                 BlacklistVehicleRepository blacklistVehicles,
                                 PatternAllowlistRepository patternAllowlist) {
        this.lots = lots;
        this.lanes = lanes;
        this.internalVehicles = internalVehicles;
        this.whitelistVehicles = whitelistVehicles;
        this.blacklistVehicles = blacklistVehicles;
        this.patternAllowlist = patternAllowlist;
    }

    @Transactional(readOnly = true)
    public AccessDecisionView decide(Long lotId, AccessDecisionRequest request) {
        ParkingLot lot = requireLot(lotId);
        requireLane(lotId, request.laneId());

        String plate = request.plateNumber().trim().toUpperCase();
        PlateColor color = request.plateColor();
        boolean isEntry = request.direction() != null
                && request.direction().equalsIgnoreCase(AccessDirection.ENTRANCE.name());
        LocalDateTime now = SiteZoneTimes.nowUtc();

        boolean whitelisted = whitelistVehicles.existsActiveAt(lotId, plate, color, now);
        boolean blacklisted = isListedBlack(lotId, plate, color);
        boolean interceptBlacklisted = isEntry ? lot.isEntryInterceptBlacklist() : lot.isExitInterceptBlacklist();
        boolean patternMatched = matchesPattern(lotId, plate);

        for (AccessJudgmentRuleType rule : lot.effectiveJudgmentOrder()) {
            if (rule == AccessJudgmentRuleType.WHITELIST && whitelisted) {
                return AccessDecisionView.allowed("whitelist_match");
            }
            if (rule == AccessJudgmentRuleType.BLACKLIST && interceptBlacklisted && blacklisted) {
                return AccessDecisionView.intercepted("blacklisted_vehicle");
            }
            if (rule == AccessJudgmentRuleType.PATTERN_ALLOWLIST && patternMatched) {
                return AccessDecisionView.allowed("pattern_allowlist_match");
            }
        }

        // 内部车场入场：必须已登记内部车辆。
        if (isEntry
                && lot.getLotType() == LotType.INTERNAL
                && !isListedInternal(lotId, plate, color)) {
            return AccessDecisionView.intercepted("not_internal_vehicle");
        }

        // 通道配置的拦截车牌颜色（由调用方提供）。
        if (request.plateColor() != null
                && request.interceptColors() != null
                && request.interceptColors().contains(request.plateColor())) {
            return AccessDecisionView.intercepted("plate_color_intercept");
        }

        // 出场但无在场流水：允许放行并在 remark 中标记。
        if (!isEntry && Boolean.FALSE.equals(request.hasOpenSession())) {
            return AccessDecisionView.allowed("no_open_session");
        }

        return AccessDecisionView.allowed("");
    }

    private boolean isListedBlack(Long lotId, String plate, PlateColor color) {
        if (color == null) {
            return blacklistVehicles.existsByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(lotId, plate);
        }
        return blacklistVehicles.existsByLotIdAndPlateNumberIgnoreCaseAndPlateColorAndEnabledTrue(
                lotId, plate, color);
    }

    private boolean isListedInternal(Long lotId, String plate, PlateColor color) {
        if (color == null) {
            return internalVehicles.existsByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(lotId, plate);
        }
        return internalVehicles.existsByLotIdAndPlateNumberIgnoreCaseAndPlateColorAndEnabledTrue(
                lotId, plate, color);
    }

    private boolean matchesPattern(Long lotId, String plate) {
        for (PatternAllowlist entry : patternAllowlist.findByLotIdAndEnabledTrue(lotId)) {
            try {
                if (Pattern.compile(entry.getPattern()).matcher(plate).find()) {
                    return true;
                }
            } catch (PatternSyntaxException ignored) {
                // 规则保存时已校验，历史脏数据直接跳过。
            }
        }
        return false;
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private void requireLane(Long lotId, Long laneId) {
        if (laneId == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingLane lane = lanes.findById(laneId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        if (!lane.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
    }
}
