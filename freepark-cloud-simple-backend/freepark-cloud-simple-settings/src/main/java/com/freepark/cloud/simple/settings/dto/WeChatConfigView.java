package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 微信支付配置视图（管理端，单个统一响应）。
 *
 * @param mchId             微信支付商户号
 * @param mchName           商户名称
 * @param mchApiKeySet      商户 API 密钥是否已配置（密钥不回显）
 * @param mchPrivateKeySet  商户 API 证书私钥是否已上传（不回显）
 * @param mchCertSerialNo   商户序列号（证书序列号）
 * @param mchCertIssuer     商户 API 证书颁发者（解析展示）
 * @param mchCertValidUntil 商户 API 证书有效期截止（解析展示）
 * @param mpAppId           缴费授权公众号 AppID
 * @param mpAppSecretSet    公众号 AppSecret 是否已配置（密钥不回显）
 * @param updatedAt         最近更新时间
 * @param notifyUrl         当前生效的支付回调地址（自定义或默认）
 * @param defaultNotifyUrl  系统默认回调地址（后台基础地址拼接，供一键恢复）
 */
public record WeChatConfigView(
        String mchId,
        String mchName,
        boolean mchApiKeySet,
        boolean mchPrivateKeySet,
        String mchCertSerialNo,
        String mchCertIssuer,
        LocalDate mchCertValidUntil,
        String mpAppId,
        boolean mpAppSecretSet,
        LocalDateTime updatedAt,
        String notifyUrl,
        String defaultNotifyUrl) {
}
