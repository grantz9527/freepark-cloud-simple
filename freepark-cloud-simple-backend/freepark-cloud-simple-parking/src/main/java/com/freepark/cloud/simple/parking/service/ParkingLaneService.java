package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.parking.dto.CreateLaneRequest;
import com.freepark.cloud.simple.parking.dto.LaneView;
import com.freepark.cloud.simple.parking.dto.UpdateLaneRequest;
import com.freepark.cloud.simple.parking.entity.LaneType;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.edge.EdgeDomainChangeNotifier;
import com.freepark.cloud.simple.parking.repository.ParkingLaneRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 通道服务。
 */
@Service
public class ParkingLaneService {

    private final ParkingLotRepository lots;
    private final ParkingLaneRepository lanes;
    private final AdminGuard adminGuard;
    private final EdgeDomainChangeNotifier notifier;

    public ParkingLaneService(ParkingLotRepository lots,
                              ParkingLaneRepository lanes,
                              AdminGuard adminGuard,
                              EdgeDomainChangeNotifier notifier) {
        this.lots = lots;
        this.lanes = lanes;
        this.adminGuard = adminGuard;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public List<LaneView> listLanes(Long lotId) {
        List<ParkingLane> items = lotId == null
                ? lanes.findAllByOrderByCreatedAtDesc()
                : lanes.findAllByLot_IdOrLinkedLot_IdOrderByCreatedAtDesc(lotId, lotId);
        return items.stream().map(LaneView::from).toList();
    }

    @Transactional
    public LaneView createLane(CreateLaneRequest request) {
        adminGuard.requireEnabledAdmin();
        if (request == null || request.lotId() == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingLot lot = requireLot(request.lotId());
        ParkingLot linkedLot = resolveLinkedLot(request.lotId(), request.linkedLotId());
        String code = normalizeRequired(request.code());
        if (lanes.existsByCodeIgnoreCase(code)) {
            throw new BizException(400, MessageKeys.PARKING_LANE_CODE_EXISTS);
        }
        String name = normalizeRequired(request.name());
        boolean enabled = request.enabled() == null || request.enabled();
        LaneType laneType = request.laneType() == null ? LaneType.ENTRANCE : request.laneType();

        ParkingLane lane = new ParkingLane();
        lane.setLot(lot);
        lane.setLinkedLot(linkedLot);
        lane.setName(name);
        lane.setCode(code);
        lane.setLaneType(laneType);
        lane.setEnabled(enabled);
        ParkingLane saved = lanes.save(lane);
        notifier.upsert(EdgeConfigSyncProtocol.DOMAIN_LANE, lot.getCode(), saved);
        return LaneView.from(saved);
    }

    @Transactional
    public LaneView updateLane(Long laneId, UpdateLaneRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLane lane = requireLane(laneId);
        if (request == null || request.lotId() == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingLot lot = requireLot(request.lotId());
        ParkingLot linkedLot = resolveLinkedLot(request.lotId(), request.linkedLotId());
        boolean enabled = request.enabled() == null ? lane.isEnabled() : request.enabled();
        LaneType laneType = request.laneType() == null ? lane.getLaneType() : request.laneType();
        lane.setLot(lot);
        lane.setLinkedLot(linkedLot);
        lane.setName(normalizeRequired(request.name()));
        lane.setLaneType(laneType);
        lane.setEnabled(enabled);
        ParkingLane saved = lanes.save(lane);
        notifier.upsert(EdgeConfigSyncProtocol.DOMAIN_LANE, lot.getCode(), saved);
        return LaneView.from(saved);
    }

    private ParkingLot resolveLinkedLot(Long lotId, Long linkedLotId) {
        if (linkedLotId == null) {
            return null;
        }
        if (linkedLotId.equals(lotId)) {
            throw new BizException(400, MessageKeys.PARKING_LANE_LOTS_DUPLICATE);
        }
        return requireLot(linkedLotId);
    }

    private ParkingLane requireLane(Long laneId) {
        return lanes.findById(laneId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String normalizeRequired(String value) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }
}
