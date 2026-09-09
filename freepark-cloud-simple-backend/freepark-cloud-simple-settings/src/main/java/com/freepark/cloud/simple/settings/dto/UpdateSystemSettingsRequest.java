package com.freepark.cloud.simple.settings.dto;

import java.util.List;

/**
 * 更新站点配置请求（当前包含区域与语言、车牌版式、车牌颜色、收费金额单位、收费方式五类配置）。
 *
 * @param defaultLocale          默认语言（zh-CN / en）
 * @param timezone               默认时区（IANA）
 * @param plateRegion            站点默认车牌版式（国家/地区，须在受支持集合内）
 * @param defaultPlateColor      默认车牌颜色（须在 allowedPlateColors 内）
 * @param allowedPlateColors     允许的车牌颜色（至少一种，须在受支持集合内）
 * @param defaultCurrency        默认币种（收费金额单位，须在 allowedCurrencies 内）
 * @param allowedCurrencies      允许的币种（至少一种，须在受支持集合内）
 * @param allowedPaymentMethods  允许的收费方式（至少一种，须在受支持集合内）
 */
public record UpdateSystemSettingsRequest(
        String defaultLocale,
        String timezone,
        String plateRegion,
        String defaultPlateColor,
        List<String> allowedPlateColors,
        String defaultCurrency,
        List<String> allowedCurrencies,
        List<String> allowedPaymentMethods) {
}
