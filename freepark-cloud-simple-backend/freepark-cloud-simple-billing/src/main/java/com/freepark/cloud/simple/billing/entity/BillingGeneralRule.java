package com.freepark.cloud.simple.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 24 小时制计费规则（全局计费模板，供各车场通过「车场计费配置」手动选用）。
 * <p>
 * 规则模板本身不直接绑定车场或车牌颜色：同一模板可被多个车场引用，同一车场也可在不同
 * 生效时段引用不同模板（甚至分别选用每日制与 24 小时制）。车场侧在「计费配置」中
 * 选择模板并指定车牌颜色（空 = 默认）与生效起止日期，见 {@link BillingLotBinding}。
 * </p>
 * <p>
 * 计费口径：以 24 小时为一个计费周期，周期内按「计费周期时长（分钟）× 周期单价」累计，
 * 累计金额以「每 24 小时封顶金额」为上限（0 表示不封顶）。
 * 免费时长：周期内产生费用前先享受的免费时长；再次计费时长：场内缴费后离场宽限，
 * 在该时长内再次产生费用时不重复计费。周六日 / 假期是否免费按收费发生时刻所在自然日单独判定。
 * 计价方式二选一：{@code cycleProfileId} 为空时按「周期时长 × 周期单价」固定计费；
 * 非空时选用全局「计费周期方案」的分段费率计费（详见 BillingCycleProfile）。
 * </p>
 */
@Entity
@Table(name = "billing_general_rule")
public class BillingGeneralRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 规则标题 */
    @Column(nullable = false, length = 80)
    private String title;

    /** 规则描述（可选） */
    @Column(length = 255)
    private String description;

    /** 免费时长（分钟），0 表示无免费 */
    @Column(nullable = false)
    private int freeMinutes;

    /** 再次计费时长（分钟）：缴费后离场宽限，0 表示无宽限 */
    @Column(nullable = false)
    private int graceMinutes;

    /** 周六/周日是否免费 */
    @Column(nullable = false)
    private boolean weekendFree;

    /** 假期（节假日时间段）是否免费 */
    @Column(nullable = false)
    private boolean holidayFree;

    /** 计费周期时长（分钟），如 60 = 每小时一个计费档 */
    @Column(nullable = false)
    private int cycleMinutes;

    /** 每个计费周期单价（元），对应 cycleMinutes 分钟 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceYuan = BigDecimal.ZERO;

    /** 每 24 小时封顶金额（元），0 表示不封顶 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal capPer24hYuan = BigDecimal.ZERO;

    /** 计费周期方案 id（逻辑关联 billing_cycle_profile.id）。
     * 为空表示按上方固定周期（cycleMinutes × unitPriceYuan）计费；
     * 非空表示选用该全局方案的分段计费，固定周期字段被忽略。 */
    private Long cycleProfileId;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(int freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public int getGraceMinutes() {
        return graceMinutes;
    }

    public void setGraceMinutes(int graceMinutes) {
        this.graceMinutes = graceMinutes;
    }

    public boolean isWeekendFree() {
        return weekendFree;
    }

    public void setWeekendFree(boolean weekendFree) {
        this.weekendFree = weekendFree;
    }

    public boolean isHolidayFree() {
        return holidayFree;
    }

    public void setHolidayFree(boolean holidayFree) {
        this.holidayFree = holidayFree;
    }

    public int getCycleMinutes() {
        return cycleMinutes;
    }

    public void setCycleMinutes(int cycleMinutes) {
        this.cycleMinutes = cycleMinutes;
    }

    public BigDecimal getUnitPriceYuan() {
        return unitPriceYuan;
    }

    public void setUnitPriceYuan(BigDecimal unitPriceYuan) {
        this.unitPriceYuan = unitPriceYuan;
    }

    public BigDecimal getCapPer24hYuan() {
        return capPer24hYuan;
    }

    public void setCapPer24hYuan(BigDecimal capPer24hYuan) {
        this.capPer24hYuan = capPer24hYuan;
    }

    public Long getCycleProfileId() {
        return cycleProfileId;
    }

    public void setCycleProfileId(Long cycleProfileId) {
        this.cycleProfileId = cycleProfileId;
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
