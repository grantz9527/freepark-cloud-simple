package com.freepark.cloud.simple.settings.dto;

import org.springframework.util.StringUtils;

/**
 * 支付宝运行时凭据（不对外回显）：下单签名用应用私钥，回调验签用支付宝公钥。
 */
public record AlipayPayRuntimeConfig(String appId, String appPrivateKey, String alipayPublicKey) {

    public static AlipayPayRuntimeConfig empty() {
        return new AlipayPayRuntimeConfig("", "", "");
    }

    /** 异步通知验签：AppID + 支付宝公钥。 */
    public boolean ready() {
        return StringUtils.hasText(appId) && StringUtils.hasText(alipayPublicKey);
    }

    /** 真实下单：还需应用私钥。 */
    public boolean readyToPay() {
        return ready() && StringUtils.hasText(appPrivateKey);
    }
}
