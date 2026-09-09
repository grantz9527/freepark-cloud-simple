package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站点配置视图：覆盖已实现的“区域与语言 + 车牌版式 + 车牌颜色 + 收费金额单位”分组。
 *
 * @param defaultLocale         默认语言（zh-CN / en）
 * @param timezone              默认时区（IANA）
 * @param plateRegion           站点默认车牌版式（国家/地区，如 CN/HK/EU/GB/US/JP…）
 * @param defaultPlateColor     默认车牌颜色
 * @param allowedPlateColors    允许的车牌颜色（有序）
 * @param allowedPaymentMethods  允许的收费方式（有序）
 * @param supportedLocales       可选的默认语言
 * @param supportedTimezones     可选的时区
 * @param supportedPlateRegions  可选的车牌版式（国家/地区）
 * @param supportedPlateColors   可选的车牌颜色
 * @param supportedCurrencies    可选的币种
 * @param supportedPaymentMethods 可选的收费方式
 * @param updatedAt              最近更新时间
 */
public record SystemSettingsView(
        String defaultLocale,
        String timezone,
        String plateRegion,
        String defaultPlateColor,
        List<String> allowedPlateColors,
        String defaultCurrency,
        List<String> allowedCurrencies,
        List<String> allowedPaymentMethods,
        List<String> supportedLocales,
        List<String> supportedTimezones,
        List<String> supportedPlateRegions,
        List<String> supportedPlateColors,
        List<String> supportedCurrencies,
        List<String> supportedPaymentMethods,
        LocalDateTime updatedAt) {
}
