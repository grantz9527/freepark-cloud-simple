package com.freepark.cloud.simple.settings.runtime;

import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 边缘计算 MQTT 常驻连接管理器：按最新配置维护到远端 Broker 的长连接。
 *
 * <ul>
 *   <li>建连：配置“启用”且具备 Broker 参数时建立连接（cleanSession + Paho 自动重连）；</li>
 *   <li>指纹重建：连接相关参数（Broker/ClientId/凭据/保活等）变化后先释放旧连接再重建；</li>
 *   <li>退避重试：首次连接失败后按 2s→60s 指数退避自动补连，成功后复位；</li>
 *   <li>释放：配置被禁用或应用关闭时取消重试并断开释放。</li>
 * </ul>
 *
 * <p>连接变更触发点：启动对齐一次、配置保存成功（事务提交）后立即对齐、
 * 以及首次连接失败的自愈补连。连接丢失后的自动恢复由 Paho 内建处理。</p>
 */
@Component
public class EdgeMqttConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(EdgeMqttConnectionManager.class);

    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final long RETRY_BASE_SECONDS = 2L;
    private static final long RETRY_MAX_SECONDS = 60L;

    /** 上行心跳订阅 QoS：至少一次，避免偶发丢包导致在线误判离线 */
    private static final int HEARTBEAT_SUBSCRIBE_QOS = 1;

    private final EdgeMqttConfigService configService;
    /** 入站消息消费方（心跳监控等），每条订阅主题送达消息都会扇出给它们 */
    private final List<EdgeInboundConsumer> inboundConsumers;

    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "edge-mqtt-reconnect");
                thread.setDaemon(true);
                return thread;
            });

    /** 当前持有的连接；仅同步块内访问 */
    private MqttClient client;
    /** 当前连接的配置指纹；空串表示“无需连接” */
    private String fingerprint = "";
    /** 当前已成功订阅的主题过滤器 → QoS（用于退订/重建时做差量对齐） */
    private final Map<String, Integer> appliedSubscriptions = new HashMap<>();
    private int retryAttempts = 0;
    private ScheduledFuture<?> retryFuture;

    public EdgeMqttConnectionManager(EdgeMqttConfigService configService,
            List<EdgeInboundConsumer> inboundConsumers) {
        this.configService = configService;
        this.inboundConsumers = inboundConsumers;
    }

    /** 启动时对齐一次：配置若已启用则尽快建立连接 */
    @PostConstruct
    public void start() {
        try {
            reconcile();
        } catch (RuntimeException e) {
            log.warn("边缘 MQTT 启动对齐失败，将按退避策略自动重试：{}",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /** 配置保存成功（事务提交）后立即对齐连接 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfigChanged(EdgeMqttConfigChangedEvent event) {
        try {
            reconcile();
        } catch (RuntimeException e) {
            log.warn("边缘 MQTT 配置变更对齐失败，将按退避策略自动重试：{}",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        reconnectExecutor.shutdownNow();
        dispose();
    }

    /**
     * 以最新配置为准对齐连接：参数没变且已连接时为空操作。
     * 可从定时下发周期等处周期调用，用于配置事件丢失等场景的自愈。
     */
    public synchronized void reconcile() {
        EdgeMqttConfig config = configService.runtimeConfig();
        String desired = desiredFingerprint(config);
        if (desired.isEmpty()) {
            if (client != null) {
                log.info("边缘计算未启用或缺少 Broker 参数，释放 MQTT 连接");
            }
            dispose();
            return;
        }
        if (desired.equals(fingerprint) && client != null && client.isConnected()) {
            retryAttempts = 0;
            cancelRetryIfAny();
            // 主题类配置变更（心跳订阅主题等）不参与指纹，需在已连接状态下补一次订阅差量对齐
            subscribeCurrent(config);
            return;
        }
        if (desired.equals(fingerprint)) {
            // 参数未变但当前未连接：直接补连，保留退避计数（由 dispose 重置会打断递增）
            connect(config);
            return;
        }
        dispose();
        fingerprint = desired;
        connect(config);
    }

    /** 已启用且当前连接可用时返回 true（供调度前置判断，不触发重连） */
    public synchronized boolean isReady() {
        return client != null && client.isConnected();
    }

    /**
     * 通过当前连接发布一条消息。
     *
     * @return true=发布成功；false=当前未连接或发布异常（调用方决定跳过/告警）
     */
    public boolean publish(String topic, byte[] payload, int qos, boolean retained) {
        MqttClient target;
        synchronized (this) {
            target = (client != null && client.isConnected()) ? client : null;
        }
        if (target == null) {
            return false;
        }
        try {
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            message.setRetained(retained);
            target.publish(topic, message);
            return true;
        } catch (MqttException e) {
            log.warn("边缘 MQTT 发布失败 topic={}：{}", topic, describe(e));
            return false;
        }
    }

    private void connect(EdgeMqttConfig config) {
        String brokerUri = "tcp://" + config.getBrokerHost() + ":" + config.getBrokerPort();
        MqttClient candidate = null;
        try {
            candidate = new MqttClient(brokerUri, config.getClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(CONNECT_TIMEOUT_SECONDS);
            options.setKeepAliveInterval(config.getKeepAliveSeconds());
            // 连接建立后的意外断开由 Paho 自动重连（自带指数退避）
            options.setAutomaticReconnect(true);
            options.setMaxReconnectDelay((int) TimeUnit.SECONDS.toMillis(RETRY_MAX_SECONDS));
            String username = config.getUsername();
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                String password = config.getPassword();
                options.setPassword(password == null ? new char[0] : password.toCharArray());
            }
            candidate.setCallback(new EdgeMqttCallback());
            candidate.connect(options);
            this.client = candidate;
            retryAttempts = 0;
            cancelRetryIfAny();
            log.info("边缘 MQTT 已连接：{}（clientId={}）", brokerUri, config.getClientId());
            subscribeCurrent(config);
        } catch (MqttException e) {
            closeFailed(candidate);
            log.warn("边缘 MQTT 连接失败 {}：{}，将在 {} 秒后重试", brokerUri, describe(e), nextRetryDelay());
            scheduleRetry();
        } catch (RuntimeException e) {
            closeFailed(candidate);
            log.warn("边缘 MQTT 连接失败 {}：{}，将在 {} 秒后重试", brokerUri, describe(e), nextRetryDelay());
            scheduleRetry();
        }
    }

    private static void closeFailed(MqttClient candidate) {
        if (candidate == null) {
            return;
        }
        try {
            candidate.close();
        } catch (MqttException ignored) {
            // 关闭失败的半成品连接无副作用
        }
    }

    private void scheduleRetry() {
        if (retryFuture != null) {
            return;
        }
        long delay = nextRetryDelay();
        retryAttempts++;
        retryFuture = reconnectExecutor.schedule(() -> {
            synchronized (EdgeMqttConnectionManager.this) {
                retryFuture = null;
                try {
                    reconcile();
                } catch (RuntimeException e) {
                    log.warn("边缘 MQTT 退避重连异常：{}",
                            e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                }
            }
        }, delay, TimeUnit.SECONDS);
    }

    private long nextRetryDelay() {
        // 2s → 4s → 8s → 16s → 32s → 60s（封顶）
        return Math.min(RETRY_BASE_SECONDS << Math.min(retryAttempts, 5), RETRY_MAX_SECONDS);
    }

    private void cancelRetryIfAny() {
        if (retryFuture != null) {
            retryFuture.cancel(false);
            retryFuture = null;
        }
    }

    private synchronized void dispose() {
        cancelRetryIfAny();
        if (client != null) {
            MqttClient old = client;
            client = null;
            try {
                old.disconnect();
            } catch (MqttException ignored) {
                // 未完成建连时断开无副作用
            }
            try {
                old.close();
            } catch (MqttException ignored) {
                // 关闭失败同样视为已释放
            }
            log.info("边缘 MQTT 连接已释放");
        }
        appliedSubscriptions.clear();
        retryAttempts = 0;
        fingerprint = "";
    }

    /** 只有连接相关参数参与指纹；周期/QoS/订阅主题变化无需重建连接 */
    private static String desiredFingerprint(EdgeMqttConfig config) {
        if (!config.isEnabled() || config.getBrokerHost() == null || config.getBrokerHost().isBlank()) {
            return "";
        }
        return String.valueOf(Objects.hash(
                config.getBrokerHost(),
                config.getBrokerPort(),
                config.getClientId(),
                config.getUsername(),
                config.getPassword(),
                config.getKeepAliveSeconds()));
    }

    /**
     * 按最新配置对当前已连接客户端做订阅差量对齐：订阅心跳主题过滤器，
     * 退订已失效的旧过滤器。首次建连与自动重连（cleanSession）后都会经此恢复订阅。
     */
    private synchronized void subscribeCurrent(EdgeMqttConfig config) {
        if (client == null || !client.isConnected()) {
            return;
        }
        Map<String, Integer> desired = desiredSubscriptions(config);
        // 退订已不再需要/已变更的过滤器
        for (Map.Entry<String, Integer> entry : appliedSubscriptions.entrySet()) {
            if (!desired.containsKey(entry.getKey())) {
                try {
                    client.unsubscribe(entry.getKey());
                    log.info("边缘 MQTT 已退订主题：{}", entry.getKey());
                } catch (MqttException e) {
                    log.warn("边缘 MQTT 退订失败 topic={}：{}", entry.getKey(), describe(e));
                }
            }
        }
        // 订阅新增的过滤器
        for (Map.Entry<String, Integer> entry : desired.entrySet()) {
            if (appliedSubscriptions.containsKey(entry.getKey())) {
                continue;
            }
            try {
                client.subscribe(entry.getKey(), entry.getValue());
                appliedSubscriptions.put(entry.getKey(), entry.getValue());
                log.info("边缘 MQTT 已订阅主题：{}（QoS {}）", entry.getKey(), entry.getValue());
            } catch (MqttException e) {
                log.warn("边缘 MQTT 订阅失败 topic={}：{}", entry.getKey(), describe(e));
            }
        }
    }

    /** 期望订阅集合：当前仅“上行心跳监控”订阅心跳订阅主题（QoS 1 保在线判定可靠） */
    private static Map<String, Integer> desiredSubscriptions(EdgeMqttConfig config) {
        Map<String, Integer> desired = new HashMap<>();
        if (!config.isEnabled()) {
            return desired;
        }
        String heartbeatTopic = config.getHeartbeatSubscribeTopic();
        if (heartbeatTopic != null && !heartbeatTopic.isBlank()) {
            desired.put(heartbeatTopic, HEARTBEAT_SUBSCRIBE_QOS);
        }
        return desired;
    }

    private static String describe(MqttException e) {
        return e.getMessage() == null ? "MQTT reason code " + e.getReasonCode() : e.getMessage();
    }

    private static String describe(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /** Paho 回调：断线日志；自动重连完成后恢复订阅；送达消息扇出给入站消费方 */
    private final class EdgeMqttCallback implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (!reconnect) {
                return; // 首次建连成功后已由 connect() 主动补齐订阅
            }
            log.info("边缘 MQTT 自动重连成功：{}，恢复订阅", serverURI);
            try {
                subscribeCurrent(configService.runtimeConfig());
            } catch (RuntimeException e) {
                log.warn("边缘 MQTT 重连后恢复订阅失败：{}", describe(e));
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            log.warn("边缘 MQTT 连接断开，Paho 将自动重连：{}", describe(cause));
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            byte[] payload = message.getPayload();
            for (EdgeInboundConsumer consumer : inboundConsumers) {
                try {
                    consumer.onMessage(topic, payload);
                } catch (RuntimeException e) {
                    log.warn("边缘 MQTT 入站消息处理异常 consumer={} topic={}：{}",
                            consumer.getClass().getSimpleName(), topic, describe(e));
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // 无需处理
        }
    }
}
