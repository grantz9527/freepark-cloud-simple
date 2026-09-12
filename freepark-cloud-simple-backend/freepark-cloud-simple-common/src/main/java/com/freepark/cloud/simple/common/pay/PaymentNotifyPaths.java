package com.freepark.cloud.simple.common.pay;

/**
 * 微信 / 支付宝支付异步通知路径（相对云端根，需公网 HTTPS）。
 * 免鉴权：挂在 {@code /api/public/**} 下。
 */
public final class PaymentNotifyPaths {

    private PaymentNotifyPaths() {
    }

    /** 微信支付 APIv3 支付结果通知 */
    public static final String WECHAT = "/api/public/payment/wechat/notify";

    /** 支付宝异步通知 */
    public static final String ALIPAY = "/api/public/payment/alipay/notify";

    /** C 端缴款结果页（用户端基础地址 + /pay/{payNo}，供渠道 return_url） */
    public static final String USER_PAY_PREFIX = "/pay/";
}
