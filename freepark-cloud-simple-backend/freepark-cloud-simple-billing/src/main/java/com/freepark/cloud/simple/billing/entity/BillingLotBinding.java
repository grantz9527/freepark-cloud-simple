package com.freepark.cloud.simple.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车场计费配置（规则模板 × 车场 的启用绑定，供云端算费引擎按车场读取）。
 * <p>
 * 计费规则（每日制 / 24 小时制）是全局模板，本身不绑定车场；车场通过本表手动选用某条模板，
 * 并同时指定：适用车牌颜色（{@code plateColor} 为空 = 默认，适用未设置专属颜色绑定的所有颜色）、
 * 生效起止日期（{@code effectiveFrom}/{@code effectiveTo} 均可为空 = 不限，如长期生效可两端皆空）。
 * 一条模板可被多个车场引用，单条配置仅对应一个车场。
 * </p>
 * <p>
 * 冲突约束：同一车场、同一车牌颜色（空视为同一「默认」）的配置，其生效区间不可重叠——
 * 无论所选模板属于每日制还是 24 小时制，避免某车场在相同时间对相同车牌颜色存在两套计费口径。
 * 不同车牌颜色（含默认与专属颜色并存）的配置可同时生效，专属颜色优先于默认。
 * </p>
 * <p>
 * 与老版本的差异：早前规则直接携带 lotId + plateColor 且每车场每颜色至多一条；
 * 现拆分为「全局模板 + 本绑定」后，单条模板不再归属特定车场。
 * </p>
 */
@Entity
@Table(name = "billing_lot_binding")
public class BillingLotBinding {

    /** 绑定模板类型：每日制（对应 BillingDailyRule） */
    public static final String TYPE_DAILY = "DAILY";

    /** 绑定模板类型：24 小时制（对应 BillingGeneralRule） */
    public static final String TYPE_GENERAL = "GENERAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属车场 id（逻辑关联 parking_lot.id，不建物理外键） */
    @Column(nullable = false)
    private Long lotId;

    /** 模板类型：DAILY = 每日制计费规则；GENERAL = 24 小时制计费规则 */
    @Column(nullable = false, length = 16)
    private String ruleType;

    /** 引用的计费规则模板 id（按 ruleType 分别逻辑关联 billing_daily_rule / billing_general_rule.id） */
    @Column(nullable = false)
    private Long ruleId;

    /** 车牌颜色（空 = 默认，适用未配置专属颜色绑定的所有颜色）；取值与系统配置的车牌颜色集合一致 */
    @Column(length = 32)
    private String plateColor;

    /** 生效开始日期（含），空 = 不限过去（即长期生效、可追溯） */
    private LocalDate effectiveFrom;

    /** 生效结束日期（含），空 = 长期生效（无截止） */
    private LocalDate effectiveTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getPlateColor() {
        return plateColor;
    }

    public void setPlateColor(String plateColor) {
        this.plateColor = plateColor;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
