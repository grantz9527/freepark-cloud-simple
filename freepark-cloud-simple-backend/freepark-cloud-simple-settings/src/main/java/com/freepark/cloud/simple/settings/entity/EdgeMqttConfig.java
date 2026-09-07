package com.freepark.cloud.simple.settings.entity;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 边缘计算配置（单例）：云端连接远端 Broker 的 MQTT 参数。
 * <p>云端角色：订阅停车系统上报的数据主题；周期性将配置发布到
 * “配置同步发布主题”，供其它边缘计算服务订阅后同步到本地。</p>
 */
@Entity
@Table(name = "edge_mqtt_config")
public class EdgeMqttConfig {

    /** 单例记录主键 */
    public static final String SINGLETON_ID = "default";

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id = SINGLETON_ID;

    /** 是否启用边缘计算接入（订阅上报数据 + 周期同步配置） */
    @Column(nullable = false)
    private boolean enabled = false;

    /** MQTT Broker 主机地址（IP 或域名，不含协议前缀） */
    @Column(name = "broker_host", nullable = false, length = 255)
    private String brokerHost = "";

    /** MQTT Broker 端口（默认 1883） */
    @Column(name = "broker_port", nullable = false)
    private int brokerPort = 1883;

    /** MQTT Client ID */
    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId = "";

    /** MQTT 用户名（可选） */
    @Column(length = 128)
    private String username;

    /** MQTT 密码（可选；本地边缘服务场景明文存储，响应中不回显） */
    @Column(length = 255)
    private String password;

    /** 上报数据订阅主题：停车系统发布上报数据，云端订阅接收 */
    @Column(name = "report_subscribe_topic", length = 255)
    private String reportSubscribeTopic;

    /** 配置同步发布主题：云端发布配置，其它边缘计算服务订阅后同步到本地 */
    @Column(name = "config_sync_publish_topic", length = 255)
    private String configSyncPublishTopic;

    /** 消息服务质量（0 / 1 / 2） */
    @Column(nullable = false)
    private int qos = 0;

    /** 配置同步周期（秒）：云端定时下发配置到边缘服务的间隔 */
    @Column(name = "config_sync_interval_seconds", nullable = false)
    private int configSyncIntervalSeconds = 60;

    /** 连接保活间隔（秒） */
    @Column(name = "keep_alive_seconds", nullable = false)
    private int keepAliveSeconds = 60;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected EdgeMqttConfig() {
    }

    public EdgeMqttConfig(String brokerHost, int brokerPort, String clientId) {
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.clientId = clientId;
    }

    @PrePersist
    void prePersist() {
        this.updatedAt = SiteZoneTimes.nowUtc();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = SiteZoneTimes.nowUtc();
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public void setBrokerHost(String brokerHost) {
        this.brokerHost = brokerHost;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(int brokerPort) {
        this.brokerPort = brokerPort;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getReportSubscribeTopic() {
        return reportSubscribeTopic;
    }

    public void setReportSubscribeTopic(String reportSubscribeTopic) {
        this.reportSubscribeTopic = reportSubscribeTopic;
    }

    public String getConfigSyncPublishTopic() {
        return configSyncPublishTopic;
    }

    public void setConfigSyncPublishTopic(String configSyncPublishTopic) {
        this.configSyncPublishTopic = configSyncPublishTopic;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public int getConfigSyncIntervalSeconds() {
        return configSyncIntervalSeconds;
    }

    public void setConfigSyncIntervalSeconds(int configSyncIntervalSeconds) {
        this.configSyncIntervalSeconds = configSyncIntervalSeconds;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
