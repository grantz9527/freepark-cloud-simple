package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 微信支付配置视图（管理端，单个统一响应）。
 *
 * @param mchId                  微信支付商户号
 * @param mchApiKeySet           商户 API 密钥是否已配置（密钥不回显）
 * @param mchPrivateKeySet       商户 API 证书私钥是否已上传（不回显）
 * @param mchCertSerialNo        商户 API 证书序列号（上传解析，十六进制大写）
 * @param mchCertIssuer          商户 API 证书颁发者（解析展示）
 * @param mchCertValidUntil      商户 API 证书有效期截止（解析展示）
 * @param wechatPayPublicKeySet  微信支付公钥是否已配置（不回显）
 * @param wechatPayPublicKeyId   微信支付公钥 ID
 * @param mpAppId                缴费授权公众号 AppID
 * @param mpAppSecretSet         公众号 AppSecret 是否已配置（密钥不回显）
 * @param updatedAt              最近更新时间
 */
public record WeChatConfigView(
        String mchId,
        boolean mchApiKeySet,
        boolean mchPrivateKeySet,
        String mchCertSerialNo,
        String mchCertIssuer,
        LocalDate mchCertValidUntil,
        boolean wechatPayPublicKeySet,
        String wechatPayPublicKeyId,
        String mpAppId,
        boolean mpAppSecretSet,
        LocalDateTime updatedAt) {
}
