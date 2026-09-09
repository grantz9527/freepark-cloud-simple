package com.freepark.cloud.simple.parking.dto;

/**
 * 算费请求（边缘节点 → 云端）：按车牌查询欠费金额。
 * {@code lotCode} 用于定位请求方车场并按其「欠费统计范围」统计（可不传，不传时按全部车场统计）；
 * {@code plateColor} 仅为与边缘侧既有契约对齐，统计口径不按颜色区分。
 * 参数合法性由服务层校验（空车牌返回 400）。
 */
public record EdgeArrearsQuoteRequest(
        String lotCode,
        String plateNumber,
        String plateColor) {
}
