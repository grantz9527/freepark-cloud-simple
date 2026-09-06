package com.freepark.cloud.simple.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 计费特殊时间段记录：节假日免费区间或补班按工作日计费区间（全局统一，供各车场计费规则引用）。
 * 时间语义：库内统一存「UTC 挂钟时间」作为绝对时刻锚点；读写边界由服务按「系统配置时区」换算，
 * 响应给前端的一律是配置时区的本地挂钟时间（含 createdAt/updatedAt）。
 */
@Entity
@Table(name = "billing_special_date")
public class BillingSpecialDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 特殊时段类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SpecialDateType type;

    /** 名称/说明（可选），如「国庆假期」 */
    @Column(length = 80)
    private String name;

    /** 开始时间（含），精确到分钟 */
    @Column(nullable = false)
    private LocalDateTime startTime;

    /** 结束时间（不含），精确到分钟 */
    @Column(nullable = false)
    private LocalDateTime endTime;

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

    public SpecialDateType getType() {
        return type;
    }

    public void setType(SpecialDateType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
