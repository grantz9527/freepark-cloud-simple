package com.freepark.cloud.simple.parking.entity;

/**
 * C 端在线支付方式：与「系统配置 - 收费方式」中启用的取值一一对应。
 * <ul>
 *   <li>WECHAT_PAY：微信支付（JSAPI，公众号内拉起）；</li>
 *   <li>ALIPAY_PAY：支付宝（手机网站支付）。</li>
 * </ul>
 */
public enum PaymentMethod {
    WECHAT_PAY,
    ALIPAY_PAY
}
