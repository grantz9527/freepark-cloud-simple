package com.freepark.cloud.simple.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 计费周期方案（全局共享，供各车场的 24 小时制计费规则选用，替代其固定的周期时长+单价）。
 * <p>
 * 方案按顺序由若干「计费分段」（{@link BillingCycleSegment}）组成，例如：
 * 第 1 个 60 分钟 5 元、第 2 个 60 分钟 1 元、第 3 个 60 分钟 2 元。
 * 计费时按分段顺序逐段累计；已列出的分段全部消耗完毕后，超出的时间不再产生费用
 * （金额以所选规则自身的「每 24 小时封顶」为最终上限）。
 * 停车时长不足当前分段时，尾数部分按 {@link CycleTailMode} 决定如何计价。
 * </p>
 */
@Entity
@Table(name = "billing_cycle_profile")
public class BillingCycleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 方案名称 */
    @Column(nullable = false, length = 80)
    private String name;

    /** 方案描述（可选） */
    @Column(length = 255)
    private String description;

    /** 尾数计费方式，取值见 {@link CycleTailMode} */
    @Column(nullable = false, length = 32)
    private String tailMode;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTailMode() {
        return tailMode;
    }

    public void setTailMode(String tailMode) {
        this.tailMode = tailMode;
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
