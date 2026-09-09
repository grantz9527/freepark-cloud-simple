package com.freepark.cloud.simple.settings.dto;

/**
 * C 端（用户端网页）公开读取的最小站点配置视图：仅用于免登录的查费页渲染默认车牌输入 UI 与金额币种。
 *
 * @param plateRegion    站点默认车牌版式（国家/地区，如 CN/HK/EU/GB/US/JP…）
 * @param defaultLocale  站点默认语言（zh-CN / en，供后续页面文案本地化使用）
 * @param defaultCurrency 站点默认币种（ISO 4217，如 CNY），供金额展示换算为对应货币符号
 */
public record PublicSiteSettingsView(
        String plateRegion,
        String defaultLocale,
        String defaultCurrency) {
}
