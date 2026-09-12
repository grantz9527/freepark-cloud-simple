package com.freepark.cloud.simple.parking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 微信内 JSAPI 调起支付参数（前端传给 WeixinJSBridge.invoke）。
 */
public record WeChatJsapiPayView(
        String appId,
        String timeStamp,
        String nonceStr,
        @JsonProperty("package") String packageValue,
        String signType,
        String paySign) {
}
