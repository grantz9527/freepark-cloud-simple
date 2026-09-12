package com.freepark.cloud.simple.settings.support;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 支付宝支付配置的字段长度与校验规则（开放平台应用 + RSA2 签名密钥）。
 */
public final class AlipayConfigOptions {

    private AlipayConfigOptions() {
    }

    /** 应用 AppID：支付宝开放平台应用标识（纯数字） */
    public static final int MAX_APP_ID_LENGTH = 32;

    /** 密钥/公钥文本的合理上限（内容为文本长度而非字节） */
    public static final int MAX_KEY_TEXT_LENGTH = 8192;

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    /**
     * 应用 AppID 可选；填写时只允许数字。空串表示暂未配置。
     */
    public static String validateAppId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String appId = value.trim();
        if (appId.length() > MAX_APP_ID_LENGTH
                || !appId.chars().allMatch(Character::isDigit)) {
            throw new BizException(400, MessageKeys.ALIPAY_APP_ID_INVALID);
        }
        return appId;
    }

    /**
     * 应用私钥文本：RSA2 私钥（PKCS#8 PEM）。留空返回 null 表示保持不变。
     */
    public static String validateAppPrivateKeyPem(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pem = value.trim();
        if (!pem.startsWith(BEGIN_PRIVATE_KEY) || !pem.endsWith(END_PRIVATE_KEY)) {
            throw new BizException(400, MessageKeys.ALIPAY_PRIVATE_KEY_INVALID);
        }
        if (pem.length() > MAX_KEY_TEXT_LENGTH) {
            throw new BizException(400, MessageKeys.ALIPAY_PRIVATE_KEY_INVALID);
        }
        return pem;
    }

    /**
     * 支付宝公钥内容：可为「支付宝开放平台 → 开发设置」复制的裸 Base64 文本，
     * 也可带 PUBLIC KEY PEM 头尾；须能按 RSA 公钥解析。留空返回 null 表示保持不变。
     */
    public static String validateAlipayPublicKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String content = value.trim();
        if (content.length() > MAX_KEY_TEXT_LENGTH) {
            throw new BizException(400, MessageKeys.ALIPAY_PUBLIC_KEY_INVALID);
        }
        String body = content;
        if (body.startsWith(BEGIN_PUBLIC_KEY)) {
            body = body.replace(BEGIN_PUBLIC_KEY, "").replace(END_PUBLIC_KEY, "");
        }
        String compact = body.replaceAll("\\s", "");
        if (compact.isEmpty()) {
            throw new BizException(400, MessageKeys.ALIPAY_PUBLIC_KEY_INVALID);
        }
        try {
            byte[] der = Base64.getDecoder().decode(compact);
            var publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
            if (!(publicKey instanceof RSAPublicKey)) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new BizException(400, MessageKeys.ALIPAY_PUBLIC_KEY_INVALID);
        }
        return content;
    }
}
