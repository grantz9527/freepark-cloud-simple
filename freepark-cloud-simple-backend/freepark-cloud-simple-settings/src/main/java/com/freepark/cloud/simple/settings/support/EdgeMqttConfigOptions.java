package com.freepark.cloud.simple.settings.support;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 边缘计算（MQTT）配置的默认值、取值约束与校验。
 */
public final class EdgeMqttConfigOptions {

    private EdgeMqttConfigOptions() {
    }

    public static final String DEFAULT_BROKER_HOST = "127.0.0.1";
    public static final int DEFAULT_BROKER_PORT = 1883;
    public static final String DEFAULT_CLIENT_ID = "freepark-cloud-edge";
    /** 默认 QoS：至少一次，保证配置快照不丢失；整包幂等可容忍重复 */
    public static final int DEFAULT_QOS = 1;
    public static final int DEFAULT_CONFIG_SYNC_INTERVAL_SECONDS = 60;
    public static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;
    /** 默认心跳离线判定阈值（秒）：超过该时长未收到某车场心跳即判定离线 */
    public static final int DEFAULT_HEARTBEAT_OFFLINE_SECONDS = 90;

    /** 端口合法范围（含端点） */
    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65535;

    /** 配置同步周期（秒）：云端定时向边缘服务下发配置的间隔 */
    public static final int MIN_CONFIG_SYNC_INTERVAL_SECONDS = 1;
    public static final int MAX_CONFIG_SYNC_INTERVAL_SECONDS = 604800;

    /** 连接保活间隔（秒），0 表示禁用保活 */
    public static final int MIN_KEEP_ALIVE_SECONDS = 0;
    public static final int MAX_KEEP_ALIVE_SECONDS = 65535;

    /** 心跳离线判定阈值（秒）合理范围 */
    public static final int MIN_HEARTBEAT_OFFLINE_SECONDS = 5;
    public static final int MAX_HEARTBEAT_OFFLINE_SECONDS = 86400;

    public static final List<Integer> SUPPORTED_QOS = List.of(0, 1, 2);

    public static final int MAX_HOST_LENGTH = 255;
    public static final int MAX_CLIENT_ID_LENGTH = 128;
    public static final int MAX_TOPIC_LENGTH = 255;

    /** 简化主机名校验：IP / 域名 / 主机名，不允许空白与协议前缀 */
    private static final Pattern HOST_PATTERN = Pattern.compile(
            "^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$");

    public static String validateBrokerHost(String value) {
        String host = requireText(value, MessageKeys.EDGE_CONFIG_HOST_REQUIRED);
        if (host.length() > MAX_HOST_LENGTH || !HOST_PATTERN.matcher(host).matches()) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_HOST_INVALID);
        }
        return host;
    }

    public static int validateBrokerPort(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_PORT_INVALID);
        }
        return port;
    }

    public static String validateClientId(String value) {
        String clientId = requireText(value, MessageKeys.EDGE_CONFIG_CLIENT_ID_REQUIRED);
        if (clientId.length() > MAX_CLIENT_ID_LENGTH) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_CLIENT_ID_INVALID);
        }
        return clientId;
    }

    public static int validateQos(int qos) {
        if (!SUPPORTED_QOS.contains(qos)) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_QOS_INVALID);
        }
        return qos;
    }

    public static int validateConfigSyncIntervalSeconds(int seconds) {
        if (seconds < MIN_CONFIG_SYNC_INTERVAL_SECONDS
                || seconds > MAX_CONFIG_SYNC_INTERVAL_SECONDS) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_SYNC_INTERVAL_INVALID);
        }
        return seconds;
    }

    public static int validateKeepAliveSeconds(int seconds) {
        if (seconds < MIN_KEEP_ALIVE_SECONDS || seconds > MAX_KEEP_ALIVE_SECONDS) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_KEEP_ALIVE_INVALID);
        }
        return seconds;
    }

    public static int validateHeartbeatOfflineSeconds(int seconds) {
        if (seconds < MIN_HEARTBEAT_OFFLINE_SECONDS
                || seconds > MAX_HEARTBEAT_OFFLINE_SECONDS) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_HEARTBEAT_OFFLINE_INVALID);
        }
        return seconds;
    }

    /** 可选的订阅主题：非空时仅做长度校验，允许为空（尚未确定主题时也可保存） */
    public static String normalizeOptionalTopic(String value) {
        if (value == null) {
            return null;
        }
        String topic = value.trim();
        if (topic.isEmpty()) {
            return null;
        }
        if (topic.length() > MAX_TOPIC_LENGTH) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_TOPIC_TOO_LONG);
        }
        return topic;
    }

    /**
     * 配置同步发布主题前缀：云端按车场拼接出专属主题“{prefix}/{parkCode}”再发布。
     *
     * <p>为空返回 null（保存期允许留空，运行时在未配置前缀时跳过下发周期）；
     * 非空时去除首尾空白与末尾“/”，且不允许包含空白、MQTT 通配符（# / +）
     * 或空字符——发布主题不得使用通配符，前缀含空白会导致按车场拼出的主题非法。</p>
     */
    public static String normalizeConfigSyncPublishTopicPrefix(String value) {
        if (value == null) {
            return null;
        }
        String prefix = value.trim();
        if (prefix.isEmpty()) {
            return null;
        }
        // 去掉尾部“/”，避免拼出“//”或空段主题
        int end = prefix.length();
        while (end > 0 && prefix.charAt(end - 1) == '/') {
            end--;
        }
        prefix = prefix.substring(0, end);
        if (prefix.isEmpty()) {
            return null;
        }
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (c == '\u0000' || c == '#' || c == '+' || Character.isWhitespace(c)) {
                throw new BizException(400, MessageKeys.EDGE_CONFIG_TOPIC_PREFIX_INVALID);
            }
        }
        if (prefix.length() > MAX_TOPIC_LENGTH) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_TOPIC_TOO_LONG);
        }
        return prefix;
    }

    /** 可选的凭据字段：非空时仅做长度校验 */
    public static String normalizeOptionalCredential(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String credential = value.trim();
        if (credential.isEmpty()) {
            return null;
        }
        if (credential.length() > maxLength) {
            throw new BizException(400, MessageKeys.EDGE_CONFIG_CREDENTIAL_TOO_LONG);
        }
        return credential;
    }

    private static String requireText(String value, String messageKey) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, messageKey);
        }
        return value.trim();
    }
}
