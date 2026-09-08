package com.freepark.cloud.simple.parking.edge;

import com.freepark.cloud.simple.parking.entity.EdgeNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.EdgeNodeRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeSyncTarget;
import com.freepark.cloud.simple.settings.runtime.EdgeSyncTargetSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 边缘节点作为配置下发目标：车场业务模块提供给 settings 边缘运行时（配置同步、
 * 心跳监控）的 SPI 实现。每个“已启用且编号可作为 MQTT 主题段”的节点作为一个同步
 * 目标，携带其名下绑定的全部车场编码（含停用车场，以便边缘侧同步 enabled=false）。
 */
@Component
public class EdgeNodeTargetSource implements EdgeSyncTargetSource {

    private final EdgeNodeRepository nodeRepository;
    private final ParkingLotRepository lotRepository;

    public EdgeNodeTargetSource(EdgeNodeRepository nodeRepository,
            ParkingLotRepository lotRepository) {
        this.nodeRepository = nodeRepository;
        this.lotRepository = lotRepository;
    }

    @Override
    public List<EdgeSyncTarget> targets() {
        return nodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(EdgeNode::isEnabled)
                .filter(node -> EdgeNodeCodes.isTopicSafe(node.getCode()))
                .map(node -> new EdgeSyncTarget(node.getCode(), boundParkCodes(node.getCode())))
                .toList();
    }

    private List<String> boundParkCodes(String nodeCode) {
        return lotRepository.findAllByEdgeNodeCodeOrderByCodeAsc(nodeCode).stream()
                .map(ParkingLot::getCode)
                .toList();
    }
}
