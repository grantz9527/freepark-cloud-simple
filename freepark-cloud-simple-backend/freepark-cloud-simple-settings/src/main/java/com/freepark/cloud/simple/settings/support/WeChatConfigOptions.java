package com.freepark.cloud.simple.settings.support;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;

/**
 * 微信支付配置的字段长度与校验规则。
 */
public final class WeChatConfigOptions {

    private WeChatConfigOptions() {
    }

    /** 商户号：微信支付商户号（纯数字，一般 8-10 位） */
    public static final int MAX_MCH_ID_LENGTH = 32;

    /** 商户 API 密钥：API v3 密钥（32 位字母数字） */
    public static final int MAX_MCH_API_KEY_LENGTH = 64;

    /** 公众号 AppID：形如 wx + 16 位 */
    public static final int MAX_APP_ID_LENGTH = 32;

    /** 公众号 AppSecret */
    public static final int MAX_APP_SECRET_LENGTH = 128;

    /** 商户证书序列号（去冒号后的十六进制长度） */
    public static final int MAX_SERIAL_NO_LENGTH = 64;

    /**
     * 商户证书序列号：必填项时按十六进制校验（允许带冒号/空格，入库前去除）。
     * 留空（null/空白）返回 null 表示保持不变。
     */
    public static String validateCertSerialNo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String serial = value.replaceAll("(?i)[^0-9a-f]", "");
        if (serial.isEmpty() || serial.length() > MAX_SERIAL_NO_LENGTH) {
            throw new BizException(400, MessageKeys.WECHAT_CERT_SERIAL_INVALID);
        }
        return serial;
    }

    /**
     * PEM 私钥文本：商户 API 证书私钥（apiclient_key.pem，PKCS#8）。
     * 留空返回 null 表示保持不变。
     */
    public static String validatePrivateKeyPem(String value) {
        String pem = validatePem(value, "PRIVATE KEY", MessageKeys.WECHAT_CERT_PEM_INVALID);
        return pem == null ? null : pem;
    }

    /**
     * 微信支付公钥文本（pub_key.pem，PKCS#8）。
     * 留空返回 null 表示保持不变。
     */
    public static String validateWechatPayPublicKey(String value) {
        return validatePem(value, "PUBLIC KEY", MessageKeys.WECHAT_PUBLIC_KEY_INVALID);
    }

    /** 微信支付公钥 ID：可选，留空返回 null。 */
    public static String validatePublicKeyId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String id = value.trim();
        if (id.length() > MAX_SERIAL_NO_LENGTH) {
            throw new BizException(400, MessageKeys.WECHAT_PUBLIC_KEY_ID_INVALID);
        }
        return id;
    }

    private static String validatePem(String value, String type, String messageKey) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pem = value.trim();
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        if (!pem.startsWith(begin) || !pem.endsWith(end)) {
            throw new BizException(400, messageKey);
        }
        return pem;
    }

    /**
     * 商户号必填且只允许数字。
     */
    public static String validateMchId(String value) {
        String mchId = normalizeRequired(value, MessageKeys.WECHAT_MCH_ID_REQUIRED);
        if (!mchId.chars().allMatch(Character::isDigit)) {
            throw new BizException(400, MessageKeys.WECHAT_MCH_ID_INVALID);
        }
        return mchId;
    }

    /**
     * 商户 API 密钥可选；填写时仅允许字母数字。
     */
    public static String validateMchApiKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String key = value.trim();
        if (key.length() > MAX_MCH_API_KEY_LENGTH
                || !key.chars().allMatch(c -> Character.isLetterOrDigit(c))) {
            throw new BizException(400, MessageKeys.WECHAT_MCH_API_KEY_INVALID);
        }
        return key;
    }

    /**
     * 公众号 AppID 必填且需以 wx 开头。
     */
    public static String validateMpAppId(String value) {
        String appId = normalizeRequired(value, MessageKeys.WECHAT_APP_ID_REQUIRED);
        if (!appId.startsWith("wx") || appId.length() > MAX_APP_ID_LENGTH) {
            throw new BizException(400, MessageKeys.WECHAT_APP_ID_INVALID);
        }
        return appId;
    }

    /**
     * 公众号 AppSecret 可选（敏感字段：可留空保持不变）；填写时仅限常规可见字符。
     */
    public static String validateMpAppSecret(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String secret = value.trim();
        if (secret.length() > MAX_APP_SECRET_LENGTH) {
            throw new BizException(400, MessageKeys.WECHAT_APP_SECRET_INVALID);
        }
        return secret;
    }

    private static String normalizeRequired(String value, String messageKey) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, messageKey);
        }
        return value.trim();
    }
}
