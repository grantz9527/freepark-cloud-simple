package com.freepark.cloud.simple.edge;

import java.time.Instant;
import java.util.List;

/**
 * 边缘监控-车场上行心跳在线状态视图。
 *
 * @param connectionUp            云端到 Broker 的长连接是否就绪（false=云端当前收不到任何心跳）
 * @param monitoring              心跳监控是否已启用（边缘接入启用且心跳订阅主题非空）
 * @param heartbeatSubscribeTopic 当前心跳订阅主题（监控未启用时为 null）
 * @param offlineSeconds          心跳离线判定阈值（秒）
 * @param onlineCount             在线车场数
 * @param offlineCount            离线车场数
 * @param unknownCount            从未收到过心跳的车场数
 * @param lots                    各已启用车场的心跳状态（按车场编码排序）
 */
public record EdgeHeartbeatStatusView(
        boolean connectionUp,
        boolean monitoring,
        String heartbeatSubscribeTopic,
        int offlineSeconds,
        int onlineCount,
        int offlineCount,
        int unknownCount,
        List<Item> lots) {

    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_OFFLINE = "offline";
    public static final String STATUS_UNKNOWN = "unknown";

    /**
     * 单个车场的心跳状态。
     *
     * @param parkCode   车场编码
     * @param parkName   车场名称
     * @param status     online / offline / unknown
     * @param lastSeenAt 最近一次心跳到达时刻（云端本地时钟；从未收到时为 null）
     */
    public record Item(String parkCode, String parkName, String status, Instant lastSeenAt) {
    }
}
