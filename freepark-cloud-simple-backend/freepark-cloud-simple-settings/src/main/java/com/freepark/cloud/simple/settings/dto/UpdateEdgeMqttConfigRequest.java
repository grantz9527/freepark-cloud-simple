package com.freepark.cloud.simple.settings.dto;

/**
 * 更新/测试边缘计算（MQTT）配置请求。
 * 密码契约：password 为 null 或空字符串时表示“保持不变”（密码不回显，无法与历史值区分）。
 *
 * @param enabled                   是否启用边缘计算接入
 * @param brokerHost                Broker 主机地址（IP 或域名）
 * @param brokerPort                Broker 端口
 * @param clientId                  MQTT Client ID
 * @param username                  MQTT 用户名（可空）
 * @param password                  MQTT 密码（可空；null 表示不修改）
 * @param reportSubscribeTopic      上报数据订阅主题（停车系统发布→云端订阅，可空）
 * @param configSyncPublishTopic    配置同步发布主题（云端发布→边缘服务同步到本地，可空）
 * @param qos                       消息服务质量（0/1/2）
 * @param configSyncIntervalSeconds 配置同步周期（秒）
 * @param keepAliveSeconds          保活间隔（秒）
 */
public record UpdateEdgeMqttConfigRequest(
        boolean enabled,
        String brokerHost,
        int brokerPort,
        String clientId,
        String username,
        String password,
        String reportSubscribeTopic,
        String configSyncPublishTopic,
        int qos,
        int configSyncIntervalSeconds,
        int keepAliveSeconds) {
}
