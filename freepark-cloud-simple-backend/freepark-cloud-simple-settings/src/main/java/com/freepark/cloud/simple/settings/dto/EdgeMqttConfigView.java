package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 边缘计算（MQTT）配置视图。password 仅入库、不回显。
 *
 * @param enabled                   是否启用边缘计算接入
 * @param brokerHost                Broker 主机地址
 * @param brokerPort                Broker 端口
 * @param clientId                  MQTT Client ID
 * @param username                  MQTT 用户名（可空）
 * @param reportSubscribeTopic      上报数据订阅主题（停车系统发布→云端订阅，可空）
 * @param configSyncPublishTopic    配置同步发布主题（云端发布→边缘节点同步到本地，可空）
 * @param commandPublishTopic       指令发布主题前缀（缴费开闸 + 云端流水下发；空=默认 parking/command）
 * @param heartbeatSubscribeTopic   上行心跳订阅主题（云端订阅各边缘节点心跳，末段为节点编号，可空；空=不启用心跳监控）
 * @param heartbeatOfflineSeconds   心跳离线判定阈值（秒）
 * @param qos                       消息服务质量（0/1/2）
 * @param configSyncIntervalSeconds 配置同步周期（秒）
 * @param keepAliveSeconds          保活间隔（秒）
 * @param updatedAt                 最近更新时间
 * @param supportedQos              支持的 QoS 取值
 */
public record EdgeMqttConfigView(
        boolean enabled,
        String brokerHost,
        int brokerPort,
        String clientId,
        String username,
        String reportSubscribeTopic,
        String configSyncPublishTopic,
        String commandPublishTopic,
        String heartbeatSubscribeTopic,
        int heartbeatOfflineSeconds,
        int qos,
        int configSyncIntervalSeconds,
        int keepAliveSeconds,
        LocalDateTime updatedAt,
        List<Integer> supportedQos) {
}
