package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.parking.dto.AccessJudgmentView;
import com.freepark.cloud.simple.parking.dto.CreateLotRequest;
import com.freepark.cloud.simple.parking.dto.LotInterceptView;
import com.freepark.cloud.simple.parking.dto.LotView;
import com.freepark.cloud.simple.parking.dto.UpdateAccessJudgmentRequest;
import com.freepark.cloud.simple.parking.dto.UpdateLotInterceptRequest;
import com.freepark.cloud.simple.parking.dto.UpdateLotRequest;
import com.freepark.cloud.simple.parking.entity.InterceptRuleType;
import com.freepark.cloud.simple.parking.entity.LotType;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 停车场服务：车场基础资料、出入口拦截配置、通行判定顺序。
 */
@Service
public class ParkingLotService {

    private final ParkingLotRepository lots;
    private final AdminGuard adminGuard;

    public ParkingLotService(ParkingLotRepository lots, AdminGuard adminGuard) {
        this.lots = lots;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public List<LotView> listLots() {
        return lots.findAllByOrderByCreatedAtDesc().stream()
                .map(LotView::from)
                .toList();
    }

    @Transactional
    public LotView createLot(CreateLotRequest request) {
        adminGuard.requireEnabledAdmin();
        String name = normalizeRequired(request == null ? null : request.name(), "name");
        String code = normalizeRequired(request == null ? null : request.code(), "code");
        if (lots.existsByCodeIgnoreCase(code)) {
            throw new BizException(400, MessageKeys.PARKING_LOT_CODE_EXISTS);
        }
        String address = normalizeOptional(request.address());
        int totalSpaces = request.totalSpaces() == null ? 0 : Math.max(0, request.totalSpaces());
        boolean enabled = request.enabled() == null || request.enabled();
        LotType lotType = request.lotType() == null ? LotType.INTERNAL : request.lotType();

        ParkingLot lot = new ParkingLot();
        lot.setName(name);
        lot.setCode(code);
        lot.setLotType(lotType);
        lot.setAddress(address);
        lot.setTotalSpaces(totalSpaces);
        lot.setEnabled(enabled);
        return LotView.from(lots.save(lot));
    }

    @Transactional
    public LotView updateLot(Long lotId, UpdateLotRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        String name = normalizeRequired(request.name(), "name");
        String address = normalizeOptional(request.address());
        int totalSpaces = request.totalSpaces() == null ? lot.getTotalSpaces() : Math.max(0, request.totalSpaces());
        boolean enabled = request.enabled() == null ? lot.isEnabled() : request.enabled();
        LotType lotType = request.lotType() == null ? lot.getLotType() : request.lotType();
        lot.setName(name);
        lot.setLotType(lotType);
        lot.setAddress(address);
        lot.setTotalSpaces(totalSpaces);
        lot.setEnabled(enabled);
        if (request.mapData() != null) {
            lot.setMapData(request.mapData());
        }
        return LotView.from(lots.save(lot));
    }

    @Transactional(readOnly = true)
    public LotInterceptView getLotIntercept(Long lotId) {
        return LotInterceptView.from(requireLot(lotId));
    }

    @Transactional
    public LotInterceptView updateLotIntercept(Long lotId, UpdateLotInterceptRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        List<InterceptRuleType> entryRules = request.entryRules() == null ? List.of() : request.entryRules();
        List<InterceptRuleType> exitRules = request.exitRules() == null ? List.of() : request.exitRules();
        lot.setEntryInterceptArrears(entryRules.contains(InterceptRuleType.ARREARS));
        lot.setEntryInterceptBlacklist(entryRules.contains(InterceptRuleType.BLACKLIST));
        lot.setExitInterceptArrears(exitRules.contains(InterceptRuleType.ARREARS));
        lot.setExitInterceptBlacklist(exitRules.contains(InterceptRuleType.BLACKLIST));
        return LotInterceptView.from(lots.save(lot));
    }

    @Transactional(readOnly = true)
    public AccessJudgmentView getAccessJudgment(Long lotId) {
        return AccessJudgmentView.from(requireLot(lotId));
    }

    @Transactional
    public AccessJudgmentView updateAccessJudgment(Long lotId, UpdateAccessJudgmentRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        try {
            AccessJudgmentView.validateOrder(request.ruleOrder());
        } catch (IllegalArgumentException ex) {
            throw new BizException(400, MessageKeys.PARKING_ACCESS_JUDGMENT_INVALID_ORDER);
        }
        lot.setJudgmentOrder(request.ruleOrder().stream()
                .map(Enum::name)
                .reduce((a, b) -> a + "," + b)
                .orElse(ParkingLot.DEFAULT_JUDGMENT_ORDER));
        return AccessJudgmentView.from(lots.save(lot));
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String normalizeRequired(String value, String field) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
