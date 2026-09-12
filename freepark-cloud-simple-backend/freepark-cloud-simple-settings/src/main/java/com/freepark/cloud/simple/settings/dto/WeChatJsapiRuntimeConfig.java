package com.freepark.cloud.simple.settings.dto;

import org.springframework.util.StringUtils;

/**
 * 微信 JSAPI 下单所需商户/公众号运行时凭据（不对外回显）。
 */
public record WeChatJsapiRuntimeConfig(
        String mchId,
        String apiV3Key,
        String mpAppId,
        String mpAppSecret,
        String privateKeyPem,
        String certSerialNo,
        String storedNotifyUrl) {

    public static WeChatJsapiRuntimeConfig empty() {
        return new WeChatJsapiRuntimeConfig("", "", "", "", "", "", "");
    }

    /** 是否具备 JSAPI 下单与网页授权换 openid 的最小凭据。 */
    public boolean ready() {
        return StringUtils.hasText(mchId)
                && StringUtils.hasText(apiV3Key)
                && StringUtils.hasText(mpAppId)
                && StringUtils.hasText(mpAppSecret)
                && StringUtils.hasText(privateKeyPem)
                && StringUtils.hasText(certSerialNo);
    }
}
