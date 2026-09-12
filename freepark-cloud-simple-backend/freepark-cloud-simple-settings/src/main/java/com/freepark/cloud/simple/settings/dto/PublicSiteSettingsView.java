package com.freepark.cloud.simple.settings.dto;

import java.util.List;

/**
 * C 端（用户端网页）公开读取的最小站点配置视图：免登录查费/缴费页渲染所需。
 *
 * @param plateRegion            站点默认车牌版式（国家/地区，如 CN/HK/EU/GB/US/JP…）
 * @param defaultLocale          站点默认语言（zh-CN / en）
 * @param defaultCurrency        站点默认币种（ISO 4217，如 CNY）
 * @param allowedPaymentMethods  当前开放的线上缴费方式（与系统配置「收费方式」一致，如 WECHAT_PAY / ALIPAY_PAY）
 * @param userBaseUrl            用户端基础地址（公网根，可含子路径，无尾斜杠；未配置为空串，前端可回落当前页）
 * @param wechatMpAppId          缴费授权公众号 AppID（非密钥；未配置为空串，供普通浏览器拉起微信）
 * @param forcePayAll            true 用户端必须一次缴清全部欠费；false 允许勾选指定停车记录
 */
public record PublicSiteSettingsView(
        String plateRegion,
        String defaultLocale,
        String defaultCurrency,
        List<String> allowedPaymentMethods,
        String userBaseUrl,
        String wechatMpAppId,
        boolean forcePayAll) {
}
