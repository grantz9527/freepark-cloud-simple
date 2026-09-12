package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站点配置视图：覆盖已实现的“区域与语言 + 基础地址 + 车牌版式 + 车牌颜色 + 收费金额单位”分组。
 *
 * @param defaultLocale          默认语言（zh-CN / en）
 * @param timezone               默认时区（IANA）
 * @param adminBaseUrl           后台基础地址（API 公网根，可含子路径，无尾斜杠；空表示按请求 Host 推断）
 * @param userBaseUrl            用户端基础地址（C 端公网根，可含子路径，无尾斜杠；空表示回落后台基础地址）
 * @param plateRegion            站点默认车牌版式（国家/地区，如 CN/HK/EU/GB/US/JP…）
 * @param defaultPlateColor      默认车牌颜色
 * @param allowedPlateColors     允许的车牌颜色（有序）
 * @param allowedPaymentMethods  允许的收费方式（有序）
 * @param forcePayAll            用户端是否强制一次缴清该车牌全部欠费
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
        String adminBaseUrl,
        String userBaseUrl,
        String plateRegion,
        String defaultPlateColor,
        List<String> allowedPlateColors,
        String defaultCurrency,
        List<String> allowedCurrencies,
        List<String> allowedPaymentMethods,
        boolean forcePayAll,
        List<String> supportedLocales,
        List<String> supportedTimezones,
        List<String> supportedPlateRegions,
        List<String> supportedPlateColors,
        List<String> supportedCurrencies,
        List<String> supportedPaymentMethods,
        LocalDateTime updatedAt) {
}
