package com.freepark.cloud.simple.edge;

import java.time.Instant;
import java.util.List;

/**
 * 边缘监控-边缘节点上行心跳在线状态视图。
 *
 * @param connectionUp            云端到 Broker 的长连接是否就绪（false=云端当前收不到任何心跳）
 * @param monitoring              心跳监控是否已启用（边缘接入启用且心跳订阅主题非空）
 * @param heartbeatSubscribeTopic 当前心跳订阅主题（监控未启用时为 null）
 * @param offlineSeconds          心跳离线判定阈值（秒）
 * @param onlineCount             在线节点数
 * @param offlineCount            离线节点数
 * @param unknownCount            从未收到过心跳的节点数
 * @param nodes                   各参与下发的边缘节点心跳状态（按节点编号排序）
 */
public record EdgeHeartbeatStatusView(
        boolean connectionUp,
        boolean monitoring,
        String heartbeatSubscribeTopic,
        int offlineSeconds,
        int onlineCount,
        int offlineCount,
        int unknownCount,
        List<NodeItem> nodes) {

    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_OFFLINE = "offline";
    public static final String STATUS_UNKNOWN = "unknown";

    /**
     * 单个边缘节点的心跳状态。
     *
     * @param nodeCode   节点编号
     * @param nodeName   节点名称
     * @param status     online / offline / unknown
     * @param lastSeenAt 最近一次心跳到达时刻（云端本地时钟；从未收到时为 null）
     * @param lots       该节点名下绑定的车场摘要（可为空）
     */
    public record NodeItem(String nodeCode, String nodeName, String status, Instant lastSeenAt,
            List<LotItem> lots) {
    }

    /**
     * 节点名下车场的摘要信息。
     *
     * @param parkCode 车场编码
     * @param parkName 车场名称
     */
    public record LotItem(String parkCode, String parkName) {
    }
}
