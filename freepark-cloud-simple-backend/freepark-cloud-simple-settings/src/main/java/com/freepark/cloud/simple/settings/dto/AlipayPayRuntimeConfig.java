package com.freepark.cloud.simple.settings.dto;

import org.springframework.util.StringUtils;

/**
 * 支付回调验签用的支付宝运行时凭据（不对外回显）。
 */
public record AlipayPayRuntimeConfig(String appId, String alipayPublicKey) {

    public static AlipayPayRuntimeConfig empty() {
        return new AlipayPayRuntimeConfig("", "");
    }

    public boolean ready() {
        return StringUtils.hasText(appId) && StringUtils.hasText(alipayPublicKey);
    }
}
