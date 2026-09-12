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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 停车订单退款记录：每次全部/部分退款落一笔，便于对账与审计。
 * 车场、车牌、订单号以快照平铺存储，不依赖订单后续变更。
 */
@Entity
@Table(name = "parking_order_refund")
public class ParkingOrderRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务退款单号（唯一，对外可展示） */
    @Column(nullable = false, unique = true, length = 32)
    private String refundNo;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String orderNo;

    @Column(nullable = false)
    private Long sessionId;

    private Long lotId;

    @Column(length = 120)
    private String lotName;

    @Column(nullable = false, length = 32)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PlateColor plateColor;

    /** 本次退款金额（元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountYuan;

    /** 本单累计已退（含本次，元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAfterYuan;

    /** 本单剩余可退（本次之后，元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingAfterYuan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParkingRefundType refundType;

    @Column(length = 200)
    private String reason;

    private Long operatorId;

    @Column(length = 64)
    private String operatorUsername;

    @Column(length = 64)
    private String operatorNickname;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = SiteZoneTimes.nowUtc();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
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

    public BigDecimal getAmountYuan() {
        return amountYuan;
    }

    public void setAmountYuan(BigDecimal amountYuan) {
        this.amountYuan = amountYuan;
    }

    public BigDecimal getRefundedAfterYuan() {
        return refundedAfterYuan;
    }

    public void setRefundedAfterYuan(BigDecimal refundedAfterYuan) {
        this.refundedAfterYuan = refundedAfterYuan;
    }

    public BigDecimal getRemainingAfterYuan() {
        return remainingAfterYuan;
    }

    public void setRemainingAfterYuan(BigDecimal remainingAfterYuan) {
        this.remainingAfterYuan = remainingAfterYuan;
    }

    public ParkingRefundType getRefundType() {
        return refundType;
    }

    public void setRefundType(ParkingRefundType refundType) {
        this.refundType = refundType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
