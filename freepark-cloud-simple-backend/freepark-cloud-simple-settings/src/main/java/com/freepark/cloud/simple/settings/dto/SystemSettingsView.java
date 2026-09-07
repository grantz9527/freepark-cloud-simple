package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站点配置视图：覆盖已实现的“区域与语言 + 车牌颜色 + 收费金额单位”分组。
 *
 * @param defaultLocale      默认语言（zh-CN / en）
 * @param timezone           默认时区（IANA）
 * @param defaultPlateColor  默认车牌颜色
 * @param allowedPlateColors 允许的车牌颜色（有序）
 * @param defaultCurrency    默认币种（收费金额单位）
 * @param allowedCurrencies  允许的币种（有序）
 * @param supportedLocales   可选的默认语言
 * @param supportedTimezones 可选的时区
 * @param supportedPlateColors 可选的车牌颜色
 * @param supportedCurrencies 可选的币种
 * @param updatedAt          最近更新时间
 */
public record SystemSettingsView(
        String defaultLocale,
        String timezone,
        String defaultPlateColor,
        List<String> allowedPlateColors,
        String defaultCurrency,
        List<String> allowedCurrencies,
        List<String> supportedLocales,
        List<String> supportedTimezones,
        List<String> supportedPlateColors,
        List<String> supportedCurrencies,
        LocalDateTime updatedAt) {
}
