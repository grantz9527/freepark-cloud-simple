package com.freepark.cloud.simple.settings.dto;

import java.util.List;

/**
 * 更新站点配置请求（当前仅包含区域与语言、车牌颜色、收费金额单位三类配置）。
 *
 * @param defaultLocale      默认语言（zh-CN / en）
 * @param timezone           默认时区（IANA）
 * @param defaultPlateColor  默认车牌颜色（须在 allowedPlateColors 内）
 * @param allowedPlateColors 允许的车牌颜色（至少一种，须在受支持集合内）
 * @param defaultCurrency    默认币种（收费金额单位，须在 allowedCurrencies 内）
 * @param allowedCurrencies  允许的币种（至少一种，须在受支持集合内）
 */
public record UpdateSystemSettingsRequest(
        String defaultLocale,
        String timezone,
        String defaultPlateColor,
        List<String> allowedPlateColors,
        String defaultCurrency,
        List<String> allowedCurrencies) {
}
