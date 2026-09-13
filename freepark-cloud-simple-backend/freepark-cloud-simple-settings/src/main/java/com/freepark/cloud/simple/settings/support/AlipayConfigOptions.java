package com.freepark.cloud.simple.settings.support;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
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
    private static final String BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----";
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
     * 应用私钥文本：RSA2（PKCS#8）。支付宝密钥工具导出的多为无头尾裸 Base64 .txt，
     * 也接受带 {@code BEGIN PRIVATE KEY} 的 PEM。留空返回 null 表示保持不变。
     *
     * <p>不支持 PKCS#1（{@code BEGIN RSA PRIVATE KEY}）；请在密钥工具中选择 PKCS8 格式。</p>
     */
    public static String validateAppPrivateKeyPem(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String content = stripBom(value).trim();
        if (content.length() > MAX_KEY_TEXT_LENGTH) {
            throw new BizException(400, MessageKeys.ALIPAY_PRIVATE_KEY_INVALID);
        }
        if (content.contains(BEGIN_RSA_PRIVATE_KEY)) {
            throw new BizException(400, MessageKeys.ALIPAY_PRIVATE_KEY_INVALID);
        }
        String body = content;
        if (body.startsWith(BEGIN_PRIVATE_KEY)) {
            body = body.replace(BEGIN_PRIVATE_KEY, "").replace(END_PRIVATE_KEY, "");
        }
        String compact = body.replaceAll("\\s", "");
        if (compact.isEmpty()) {
            throw new BizException(400, MessageKeys.ALIPAY_PRIVATE_KEY_INVALID);
        }
        try {
            byte[] der = Base64.getDecoder().decode(compact);
            PrivateKey key = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
            if (!(key instanceof RSAPrivateCrtKey)) {
                throw new IllegalArgumentException();
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(400, MessageKeys.ALIPAY_PRIVATE_KEY_INVALID);
        }
        return content;
    }

    /**
     * 支付宝公钥内容：可为「支付宝开放平台 → 开发设置」复制的裸 Base64 文本，
     * 也可带 PUBLIC KEY PEM 头尾；须能按 RSA 公钥解析。留空返回 null 表示保持不变。
     */
    public static String validateAlipayPublicKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String content = stripBom(value).trim();
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

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}
