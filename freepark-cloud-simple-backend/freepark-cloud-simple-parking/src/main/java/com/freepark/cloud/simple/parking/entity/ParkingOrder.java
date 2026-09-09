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
 * 停车订单：每次收费请求（缴费/登记收款）生成一笔，关联一条停车流水并记录本次收取金额。
 * <p>
 * 金额口径：{@link #amountYuan} = {@link #receivableYuan} −（{@link #paidBeforeYuan} + {@link #pendingBeforeYuan}），
 * 即「当前应收 − 流水累计已支付 − 该流水未支付/待支付订单合计」，保证在场多次缴费不重复计费：
 * 第二次只需支付再次新产生的金额。订单生成时把三方快照留档，便于审计与对账。
 * 关联的车场/车牌/入场时间同样以快照字段平铺存储（无实体关联）。
 */
@Entity
@Table(name = "parking_order")
public class ParkingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务订单号（唯一，对外可展示） */
    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    /** 关联停车流水 ID */
    @Column(nullable = false)
    private Long sessionId;

    /** 下单时关联流水状态快照（OPEN 在场 / CLOSED 已出场） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ParkingSessionStatus sessionStatus;

    /** 车场 ID（快照） */
    private Long lotId;

    /** 车场名称（快照） */
    @Column(length = 120)
    private String lotName;

    /** 车牌号（快照） */
    @Column(nullable = false, length = 32)
    private String plateNumber;

    /** 车牌颜色（快照） */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PlateColor plateColor;

    /** 关联流水入场时间（快照，便于列表展示） */
    private LocalDateTime entryTime;

    /** 当前应收口径快照（元）：已出场取流水应收快照，在场按下单时点估算 */
    @Column(precision = 10, scale = 2)
    private BigDecimal receivableYuan;

    /** 下单前流水累计已支付金额（元）快照 */
    @Column(precision = 10, scale = 2)
    private BigDecimal paidBeforeYuan = BigDecimal.ZERO;

    /** 下单前该流水未支付/待支付订单金额（元）合计快照 */
    @Column(precision = 10, scale = 2)
    private BigDecimal pendingBeforeYuan = BigDecimal.ZERO;

    /** 本单应付金额（元）：应收 − 已支付 − 未付订单，支付时入账到流水累计已支付 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountYuan;

    /** 订单状态：PENDING 待支付 / PAID 已支付 / CANCELLED 已取消 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParkingOrderStatus status = ParkingOrderStatus.PENDING;

    /** 支付时间：登记为「已支付」的时刻 */
    private LocalDateTime payTime;

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

    public ParkingSessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(ParkingSessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
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

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public BigDecimal getReceivableYuan() {
        return receivableYuan;
    }

    public void setReceivableYuan(BigDecimal receivableYuan) {
        this.receivableYuan = receivableYuan;
    }

    public BigDecimal getPaidBeforeYuan() {
        return paidBeforeYuan;
    }

    public void setPaidBeforeYuan(BigDecimal paidBeforeYuan) {
        this.paidBeforeYuan = paidBeforeYuan;
    }

    public BigDecimal getPendingBeforeYuan() {
        return pendingBeforeYuan;
    }

    public void setPendingBeforeYuan(BigDecimal pendingBeforeYuan) {
        this.pendingBeforeYuan = pendingBeforeYuan;
    }

    public BigDecimal getAmountYuan() {
        return amountYuan;
    }

    public void setAmountYuan(BigDecimal amountYuan) {
        this.amountYuan = amountYuan;
    }

    public ParkingOrderStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingOrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
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
