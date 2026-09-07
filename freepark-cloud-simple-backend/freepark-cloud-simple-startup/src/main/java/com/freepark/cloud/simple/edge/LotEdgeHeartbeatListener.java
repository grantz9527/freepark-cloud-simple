package com.freepark.cloud.simple.edge;

import com.freepark.cloud.simple.parking.entity.ParkingLot;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 车场上行心跳在线状态聚合器：为“边缘监控”页面提供每个启用车场的在线状态。
 *
 * <p>判定口径与边缘配置下发目标一致（启用且编码可作为 MQTT 主题段的车场）；
 * 在线状态不依赖任何后台调度任务，每次查询按“最近一次心跳到达是否超过离线阈值”
 * 动态计算：未收到过=unknown；超过阈值=offline；否则=online。</p>
 */
@Component
public class LotEdgeHeartbeatListener {

    private final EdgeMqttConfigService configService;
    private final EdgeMqttConnectionManager connectionManager;
    private final EdgeHeartbeatReceiver heartbeatReceiver;
    private final List<EdgeSyncTargetSource> targetSources;
    private final ParkingLotRepository lotRepository;

    public LotEdgeHeartbeatListener(EdgeMqttConfigService configService,
            EdgeMqttConnectionManager connectionManager,
            EdgeHeartbeatReceiver heartbeatReceiver,
            List<EdgeSyncTargetSource> targetSources,
            ParkingLotRepository lotRepository) {
        this.configService = configService;
        this.connectionManager = connectionManager;
        this.heartbeatReceiver = heartbeatReceiver;
        this.targetSources = targetSources;
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

        // 目标车场：与配置下发同口径（启用 + 主题安全编码），顺序以来源为准
        Set<String> targetCodes = new LinkedHashSet<>();
        for (EdgeSyncTargetSource source : targetSources) {
            for (EdgeSyncTarget target : source.targets()) {
                if (target.parkCode() != null && !target.parkCode().isBlank()) {
                    targetCodes.add(target.parkCode());
                }
            }
        }
        Map<String, String> nameByCode = new HashMap<>();
        for (ParkingLot lot : lotRepository.findAllByOrderByCreatedAtDesc()) {
            nameByCode.putIfAbsent(lot.getCode(), lot.getName());
        }

        Instant now = Instant.now();
        List<EdgeHeartbeatStatusView.Item> items = new ArrayList<>(targetCodes.size());
        int online = 0;
        int offline = 0;
        int unknown = 0;
        for (String code : targetCodes) {
            Instant lastSeen = heartbeatReceiver.lastSeen(code);
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
            items.add(new EdgeHeartbeatStatusView.Item(
                    code, nameByCode.get(code), status, lastSeen));
        }
        items.sort((a, b) -> a.parkCode().compareTo(b.parkCode()));

        return new EdgeHeartbeatStatusView(
                connectionUp, monitoring, monitoring ? topic : null, offlineSeconds,
                online, offline, unknown, List.copyOf(items));
    }
}
