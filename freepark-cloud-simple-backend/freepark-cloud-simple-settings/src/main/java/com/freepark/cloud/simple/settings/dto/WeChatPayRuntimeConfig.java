package com.freepark.cloud.simple.settings.dto;

import org.springframework.util.StringUtils;

/**
 * 支付回调验签用的微信商户运行时凭据（不对外回显）。
 *
 * <p>{@code wechatPayPublicKeyPem} 为历史可选字段：旧数据若仍存公钥则继续用于回调验签；
 * 新配置页不再采集。有 APIv3 密钥即可解密通知报文。</p>
 */
public record WeChatPayRuntimeConfig(
        String mchId,
        String apiV3Key,
        String wechatPayPublicKeyPem,
        String wechatPayPublicKeyId) {

    public static WeChatPayRuntimeConfig empty() {
        return new WeChatPayRuntimeConfig("", "", "", "");
    }

    public boolean ready() {
        return StringUtils.hasText(mchId) && StringUtils.hasText(apiV3Key);
    }

    public boolean hasPlatformPublicKey() {
        return StringUtils.hasText(wechatPayPublicKeyPem);
    }
}
