package com.freepark.cloud.simple.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

/**
 * 计费周期方案中的一个分段行（从第 1 行开始按顺序消耗）。
 * <p>
 * 例如「第 2 个 60 分钟 1 元」对应 {@code seq=2, minutes=60, unitPriceYuan=1.00}。
 * 每行可配置重复次数 {@link #repeatCount}（默认 1）：计费时将本行按顺序连续展开为
 * {@code repeatCount} 个相同档位。例如第 1 行「60 分钟 5 元 ×1」+ 第 2 行「60 分钟 1 元 ×9」，
 * 等价于第 1 档 5 元、第 2~10 档各 1 元（无需手动填 10 行）。
 * 分段归属于某个计费周期方案（{@code profileId} 逻辑关联 billing_cycle_profile.id），
 * 更新方案时整组替换，因此不含时间戳。
 * </p>
 */
@Entity
@Table(name = "billing_cycle_segment")
public class BillingCycleSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属计费周期方案 id（逻辑关联，不建物理外键） */
    @Column(nullable = false)
    private Long profileId;

    /** 段序号（从 1 开始，计费按此顺序逐段消耗） */
    @Column(nullable = false)
    private int seq;

    /** 本分段时长（分钟） */
    @Column(nullable = false)
    private int minutes;

    /** 本分段单价（元），对应本段 minutes 分钟 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceYuan = BigDecimal.ZERO;

    /** 重复次数（默认 1）：计费时本行按顺序连续展开为 N 个相同档位 */
    @ColumnDefault("1")
    @Column(nullable = false)
    private int repeatCount = 1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public BigDecimal getUnitPriceYuan() {
        return unitPriceYuan;
    }

    public void setUnitPriceYuan(BigDecimal unitPriceYuan) {
        this.unitPriceYuan = unitPriceYuan;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }
}
