package com.freepark.cloud.simple.edge;

import com.freepark.cloud.simple.parking.entity.EdgeNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.EdgeNodeRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.runtime.EdgeHeartbeatReceiver;
import com.freepark.cloud.simple.settings.runtime.EdgeMqttConnectionManager;
import com.freepark.cloud.simple.settings.runtime.EdgeSyncTarget;
import com.freepark.cloud.simple.settings.runtime.EdgeSyncTargetSource;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 边缘节点上行心跳在线状态聚合器：为“边缘监控”页面提供每个参与下发节点的在线状态，
 * 并展开其名下绑定的车场摘要。
 *
 * <p>判定口径与边缘配置下发目标一致（启用的节点）；节点在线不代表其名下车场一定
 * 可用（需结合车场本地运行状态），本页仅反映“边缘节点与云端的心跳通道”。在线状态
 * 不依赖任何后台调度任务，每次查询按“最近一次心跳到达是否超过离线阈值”动态计算：
 * 未收到过=unknown；超过阈值=offline；否则=online。</p>
 */
@Component
public class EdgeHeartbeatAggregator {

    private final EdgeMqttConfigService configService;
    private final EdgeMqttConnectionManager connectionManager;
    private final EdgeHeartbeatReceiver heartbeatReceiver;
    private final List<EdgeSyncTargetSource> targetSources;
    private final EdgeNodeRepository nodeRepository;
    private final ParkingLotRepository lotRepository;

    public EdgeHeartbeatAggregator(EdgeMqttConfigService configService,
            EdgeMqttConnectionManager connectionManager,
            EdgeHeartbeatReceiver heartbeatReceiver,
            List<EdgeSyncTargetSource> targetSources,
            EdgeNodeRepository nodeRepository,
            ParkingLotRepository lotRepository) {
        this.configService = configService;
        this.connectionManager = connectionManager;
        this.heartbeatReceiver = heartbeatReceiver;
        this.targetSources = targetSources;
        this.nodeRepository = nodeRepository;
        this.lotRepository = lotRepository;
    }

    /**
     * 汇总当前心跳监控状态（查询时动态计算）。
     */
    public EdgeHeartbeatStatusView status() {
        EdgeMqttConfig config = configService.runtimeConfig();
        boolean connectionUp = connectionManager.isReady();
        String topic = config.getHeartbeatSubscribeTopic();
        boolean monitoring = config.isEnabled() && topic != null && !topic.isBlank();
        int offlineSeconds = config.getHeartbeatOfflineSeconds();

        // 目标节点及其名下车场：与配置下发同口径（启用的节点），顺序以来源为准
        Map<String, List<String>> parkCodesByNode = new LinkedHashMap<>();
        for (EdgeSyncTargetSource source : targetSources) {
            for (EdgeSyncTarget target : source.targets()) {
                if (target.nodeCode() != null && !target.nodeCode().isBlank()
                        && !parkCodesByNode.containsKey(target.nodeCode())) {
                    parkCodesByNode.put(target.nodeCode(), target.parkCodes());
                }
            }
        }
        Map<String, String> nodeNameByCode = new HashMap<>();
        for (EdgeNode node : nodeRepository.findAll()) {
            nodeNameByCode.putIfAbsent(node.getCode(), node.getName());
        }
        Map<String, String> lotNameByCode = new HashMap<>();
        for (ParkingLot lot : lotRepository.findAll()) {
            lotNameByCode.putIfAbsent(lot.getCode(), lot.getName());
        }

        Instant now = Instant.now();
        List<EdgeHeartbeatStatusView.NodeItem> items = new ArrayList<>(parkCodesByNode.size());
        int online = 0;
        int offline = 0;
        int unknown = 0;
        for (Map.Entry<String, List<String>> entry : parkCodesByNode.entrySet()) {
            String nodeCode = entry.getKey();
            Instant lastSeen = heartbeatReceiver.lastSeen(nodeCode);
            String status;
            if (lastSeen == null) {
                status = EdgeHeartbeatStatusView.STATUS_UNKNOWN;
                unknown++;
            } else if (Duration.between(lastSeen, now).getSeconds() > offlineSeconds) {
                status = EdgeHeartbeatStatusView.STATUS_OFFLINE;
                offline++;
            } else {
                status = EdgeHeartbeatStatusView.STATUS_ONLINE;
                online++;
            }
            List<EdgeHeartbeatStatusView.LotItem> lots = new ArrayList<>(entry.getValue().size());
            for (String parkCode : entry.getValue()) {
                lots.add(new EdgeHeartbeatStatusView.LotItem(
                        parkCode, lotNameByCode.get(parkCode)));
            }
            items.add(new EdgeHeartbeatStatusView.NodeItem(
                    nodeCode, nodeNameByCode.get(nodeCode), status, lastSeen, List.copyOf(lots)));
        }
        items.sort((a, b) -> a.nodeCode().compareTo(b.nodeCode()));

        return new EdgeHeartbeatStatusView(
                connectionUp, monitoring, monitoring ? topic : null, offlineSeconds,
                online, offline, unknown, List.copyOf(items));
    }
}
