package com.freepark.cloud.simple.settings.dto;

import java.util.List;

/**
 * 更新站点配置请求（当前包含区域与语言、后台/用户端基础地址、车牌版式、车牌颜色、收费金额单位、收费方式）。
 *
 * @param defaultLocale          默认语言（zh-CN / en）
 * @param timezone               默认时区（IANA）
 * @param adminBaseUrl           后台基础地址（http(s) 地址，可含子路径，可空；支付回调）
 * @param userBaseUrl            用户端基础地址（http(s) 地址，可含子路径，可空；首页/缴费页）
 * @param plateRegion            站点默认车牌版式（国家/地区，须在受支持集合内）
 * @param defaultPlateColor      默认车牌颜色（须在 allowedPlateColors 内）
 * @param allowedPlateColors     允许的车牌颜色（至少一种，须在受支持集合内）
 * @param defaultCurrency        默认币种（收费金额单位，须在 allowedCurrencies 内）
 * @param allowedCurrencies      允许的币种（至少一种，须在受支持集合内）
 * @param allowedPaymentMethods  允许的收费方式（至少一种，须在受支持集合内）
 * @param forcePayAll            用户端是否强制缴清全部欠费（false 时允许勾选指定停车记录）
 */
public record UpdateSystemSettingsRequest(
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
        Boolean forcePayAll) {
}
