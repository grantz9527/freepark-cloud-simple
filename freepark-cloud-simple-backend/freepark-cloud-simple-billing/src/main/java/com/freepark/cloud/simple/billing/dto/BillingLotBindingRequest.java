package com.freepark.cloud.simple.billing.dto;

import java.time.LocalDate;

/**
 * 车场计费配置（规则模板 × 车场绑定）的创建/更新请求。
 *
 * @param lotId         所属车场 id
 * @param ruleType      模板类型：DAILY = 每日制；GENERAL = 24 小时制
 * @param ruleId        引用的计费规则模板 id
 * @param plateColor    车牌颜色（空/空白 = 默认，适用未配置专属颜色绑定的所有颜色）
 * @param effectiveFrom 生效开始日期（含），空 = 不限过去
 * @param effectiveTo   生效结束日期（含），空 = 长期生效
 */
public record BillingLotBindingRequest(
        Long lotId,
        String ruleType,
        Long ruleId,
        String plateColor,
        LocalDate effectiveFrom,
        LocalDate effectiveTo) {
}
