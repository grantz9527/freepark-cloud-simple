package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDateTime;

/**
 * 支付宝支付配置视图（管理端，单个统一响应）。
 *
 * @param appId              支付宝开放平台应用 AppID
 * @param appPrivateKeySet   应用私钥是否已配置（密钥不回显）
 * @param alipayPublicKeySet 支付宝公钥是否已配置（不回显）
 * @param updatedAt          最近更新时间
 * @param notifyUrl          当前生效的异步通知地址（自定义或默认）
 * @param defaultNotifyUrl   系统默认回调地址（后台基础地址拼接，供一键恢复）
 */
public record AlipayConfigView(
        String appId,
        boolean appPrivateKeySet,
        boolean alipayPublicKeySet,
        LocalDateTime updatedAt,
        String notifyUrl,
        String defaultNotifyUrl) {
}
