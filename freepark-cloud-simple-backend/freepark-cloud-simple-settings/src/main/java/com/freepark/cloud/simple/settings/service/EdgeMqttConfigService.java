package com.freepark.cloud.simple.settings.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.settings.dto.EdgeMqttConfigView;
import com.freepark.cloud.simple.settings.dto.UpdateEdgeMqttConfigRequest;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.repository.EdgeMqttConfigRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeMqttConfigChangedEvent;
import com.freepark.cloud.simple.settings.support.EdgeMqttConfigOptions;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 边缘计算（MQTT）配置服务：单例读取/保存，并提供“测试连接”能力。
 *
 * <p>密码契约：密码不回显；保存时 password 为 null 或空串表示保持不变。
 * 用户名置空后（null 入库），测试连接与实际连接均不再携带凭据。</p>
 */
@Service
public class EdgeMqttConfigService {

    private static final Logger log = LoggerFactory.getLogger(EdgeMqttConfigService.class);

    private final EdgeMqttConfigRepository repository;
    private final AdminGuard adminGuard;
    private final ApplicationEventPublisher eventPublisher;

    public EdgeMqttConfigService(EdgeMqttConfigRepository repository, AdminGuard adminGuard,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.adminGuard = adminGuard;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 读取边缘计算配置（启用中的管理员可读，页面按菜单角色限制为超管）。
     */
    @Transactional(readOnly = true)
    public EdgeMqttConfigView getConfig() {
        adminGuard.requireEnabledAdmin();
        return toView(requireConfig());
    }

    /**
     * 供运行时组件（连接管理/配置下发调度）读取最新配置实体。
     * 内部调用、不校验登录态，且不对外暴露给任何 Controller。
     */
    @Transactional(readOnly = true)
    public EdgeMqttConfig runtimeConfig() {
        return requireConfig();
    }

    /**
     * 保存边缘计算配置（仅超级管理员）。
     */
    @Transactional
    public EdgeMqttConfigView updateConfig(UpdateEdgeMqttConfigRequest request) {
        requireSuperAdmin();
        EdgeMqttConfig config = requireConfig();
        applyValidated(config, request);
        // saveAndFlush：让 @PreUpdate 回写 updatedAt，响应为真实 UTC 锚点
        EdgeMqttConfigView view = toView(repository.saveAndFlush(config));
        // 事务提交后通知运行时组件（连接管理/下发调度）按最新配置对齐
        eventPublisher.publishEvent(new EdgeMqttConfigChangedEvent());
        return view;
    }

    /**
     * 使用请求中的连接参数直连远端 Broker 做连通性验证（仅超级管理员）。
     * 校验通过并成功连接后即断开，不保留常驻连接。
     */
    public void testConnection(UpdateEdgeMqttConfigRequest request) {
        requireSuperAdmin();
        String host = EdgeMqttConfigOptions.validateBrokerHost(request.brokerHost());
        int port = EdgeMqttConfigOptions.validateBrokerPort(request.brokerPort());
        String clientId = EdgeMqttConfigOptions.validateClientId(request.clientId());
        String username = EdgeMqttConfigOptions.normalizeOptionalCredential(request.username(),
                EdgeMqttConfigOptions.MAX_CLIENT_ID_LENGTH);
        String password = EdgeMqttConfigOptions.normalizeOptionalCredential(request.password(), 255);
        int keepAlive = EdgeMqttConfigOptions.validateKeepAliveSeconds(request.keepAliveSeconds());

        String brokerUri = "tcp://" + host + ":" + port;
        MqttClient client = null;
        try {
            client = new MqttClient(brokerUri, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(keepAlive);
            if (username != null) {
                options.setUserName(username);
                options.setPassword(password == null ? new char[0] : password.toCharArray());
            }
            client.connect(options);
            log.info("边缘计算 MQTT 测试连接成功：{}", brokerUri);
        } catch (MqttException e) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_TEST_FAILED,
                    describe(e));
        } catch (RuntimeException e) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_TEST_FAILED,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            if (client != null) {
                try {
                    client.disconnect();
                } catch (MqttException ignored) {
                    // 连接未建立成功时无需断开
                }
                try {
                    client.close();
                } catch (MqttException ignored) {
                    // ignore
                }
            }
        }
    }

    private void applyValidated(EdgeMqttConfig config, UpdateEdgeMqttConfigRequest request) {
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        config.setEnabled(request.enabled());
        config.setBrokerHost(EdgeMqttConfigOptions.validateBrokerHost(request.brokerHost()));
        config.setBrokerPort(EdgeMqttConfigOptions.validateBrokerPort(request.brokerPort()));
        config.setClientId(EdgeMqttConfigOptions.validateClientId(request.clientId()));
        config.setUsername(EdgeMqttConfigOptions.normalizeOptionalCredential(request.username(),
                EdgeMqttConfigOptions.MAX_CLIENT_ID_LENGTH));
        config.setReportSubscribeTopic(
                EdgeMqttConfigOptions.normalizeOptionalTopic(request.reportSubscribeTopic()));
        config.setConfigSyncPublishTopic(EdgeMqttConfigOptions.normalizeConfigSyncPublishTopicPrefix(
                request.configSyncPublishTopic()));
        config.setHeartbeatSubscribeTopic(
                EdgeMqttConfigOptions.normalizeOptionalTopic(request.heartbeatSubscribeTopic()));
        config.setHeartbeatOfflineSeconds(EdgeMqttConfigOptions.validateHeartbeatOfflineSeconds(
                request.heartbeatOfflineSeconds()));
        config.setQos(EdgeMqttConfigOptions.validateQos(request.qos()));
        config.setConfigSyncIntervalSeconds(EdgeMqttConfigOptions.validateConfigSyncIntervalSeconds(
                request.configSyncIntervalSeconds()));
        config.setKeepAliveSeconds(EdgeMqttConfigOptions.validateKeepAliveSeconds(
                request.keepAliveSeconds()));
        if (config.getPassword() == null || (request.password() != null && !request.password().isBlank())) {
            config.setPassword(EdgeMqttConfigOptions.normalizeOptionalCredential(request.password(), 255));
        }
    }

    /**
     * 返回单例配置；若记录缺失（默认行初始化前或已被手工删除）则重建默认行。
     */
    private EdgeMqttConfig requireConfig() {
        return repository.findById(EdgeMqttConfig.SINGLETON_ID)
                .orElseGet(() -> repository.save(new EdgeMqttConfig(
                        EdgeMqttConfigOptions.DEFAULT_BROKER_HOST,
                        EdgeMqttConfigOptions.DEFAULT_BROKER_PORT,
                        EdgeMqttConfigOptions.DEFAULT_CLIENT_ID)));
    }

    private EdgeMqttConfigView toView(EdgeMqttConfig config) {
        return new EdgeMqttConfigView(
                config.isEnabled(),
                config.getBrokerHost(),
                config.getBrokerPort(),
                config.getClientId(),
                config.getUsername(),
                config.getReportSubscribeTopic(),
                config.getConfigSyncPublishTopic(),
                config.getHeartbeatSubscribeTopic(),
                config.getHeartbeatOfflineSeconds(),
                config.getQos(),
                config.getConfigSyncIntervalSeconds(),
                config.getKeepAliveSeconds(),
                config.getUpdatedAt(),
                EdgeMqttConfigOptions.SUPPORTED_QOS);
    }

    private void requireSuperAdmin() {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        String role = operator.getRole();
        if (!UserAccount.ROLE_SUPER_ADMIN.equals(role)) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
    }

    private static String describe(MqttException e) {
        return e.getMessage() == null
                ? "MQTT reason code " + e.getReasonCode()
                : e.getMessage();
    }
}
