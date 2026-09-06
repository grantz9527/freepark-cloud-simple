package com.freepark.cloud.simple.settings.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站点配置视图：仅覆盖已实现的“区域与语言 + 车牌颜色”分组。
 *
 * @param defaultLocale      默认语言（zh-CN / en）
 * @param timezone           默认时区（IANA）
 * @param defaultPlateColor  默认车牌颜色
 * @param allowedPlateColors 允许的车牌颜色（有序）
 * @param supportedLocales   可选的默认语言
 * @param supportedTimezones 可选的时区
 * @param supportedPlateColors 可选的车牌颜色
 * @param updatedAt          最近更新时间
 */
public record SystemSettingsView(
        String defaultLocale,
        String timezone,
        String defaultPlateColor,
        List<String> allowedPlateColors,
        List<String> supportedLocales,
        List<String> supportedTimezones,
        List<String> supportedPlateColors,
        LocalDateTime updatedAt) {
}
