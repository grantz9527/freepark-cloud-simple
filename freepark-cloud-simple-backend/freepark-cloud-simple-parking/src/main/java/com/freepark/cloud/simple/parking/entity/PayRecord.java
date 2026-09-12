package com.freepark.cloud.simple.parking.entity;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付平台流水：每个支付请求、每个退款请求各一行。
 * 一次线上缴款可能覆盖多个车场，车场金额记在 {@link PayRecordLot}，供后续按车场拆分统计。
 */
@Entity
@Table(name = "pay_record")
public class PayRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务记录号（唯一）：线上支付沿用缴款单号，现金收款/退款用各自单号 */
    @Column(nullable = false, unique = true, length = 32)
    private String recordNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayRecordKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PayRecordPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayRecordStatus status = PayRecordStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountYuan;

    @Column(nullable = false, length = 32)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PlateColor plateColor;

    /** 线上缴款单号；退款时指向原支付请求 */
    @Column(length = 32)
    private String relatedPayNo;

    /** 关联停车订单号：现金收款或按订单退款时有值 */
    @Column(length = 32)
    private String relatedOrderNo;

    /** 关联停车订单退款单号 */
    @Column(length = 32)
    private String relatedRefundNo;

    /** 渠道交易号 / 模拟号 */
    @Column(length = 64)
    private String transactionId;

    @Column(nullable = false)
    private boolean mock;

    private Long operatorId;

    @Column(length = 64)
    private String operatorUsername;

    @Column(length = 64)
    private String operatorNickname;

    /** 成功时刻：支付成功或退款成功 */
    private LocalDateTime successTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = SiteZoneTimes.nowUtc();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = SiteZoneTimes.nowUtc();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public PayRecordKind getKind() {
        return kind;
    }

    public void setKind(PayRecordKind kind) {
        this.kind = kind;
    }

    public PayRecordPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(PayRecordPlatform platform) {
        this.platform = platform;
    }

    public PayRecordStatus getStatus() {
        return status;
    }

    public void setStatus(PayRecordStatus status) {
        this.status = status;
    }

    public BigDecimal getAmountYuan() {
        return amountYuan;
    }

    public void setAmountYuan(BigDecimal amountYuan) {
        this.amountYuan = amountYuan;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public PlateColor getPlateColor() {
        return plateColor;
    }

    public void setPlateColor(PlateColor plateColor) {
        this.plateColor = plateColor;
    }

    public String getRelatedPayNo() {
        return relatedPayNo;
    }

    public void setRelatedPayNo(String relatedPayNo) {
        this.relatedPayNo = relatedPayNo;
    }

    public String getRelatedOrderNo() {
        return relatedOrderNo;
    }

    public void setRelatedOrderNo(String relatedOrderNo) {
        this.relatedOrderNo = relatedOrderNo;
    }

    public String getRelatedRefundNo() {
        return relatedRefundNo;
    }

    public void setRelatedRefundNo(String relatedRefundNo) {
        this.relatedRefundNo = relatedRefundNo;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public String getOperatorNickname() {
        return operatorNickname;
    }

    public void setOperatorNickname(String operatorNickname) {
        this.operatorNickname = operatorNickname;
    }

    public LocalDateTime getSuccessTime() {
        return successTime;
    }

    public void setSuccessTime(LocalDateTime successTime) {
        this.successTime = successTime;
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
