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
 * C 端在线支付单：一次「按车牌把全部未结流水一次付清」的在线支付。
 * <p>
 * 下单时把该车牌的未结流水按流水拆成多笔 {@link ParkingOrder}（PENDING，{@code paymentNo} 指向本支付单），
 * 支付成功后再把拆出的停车订单逐笔入账到对应流水的累计已支付，实现
 * 「一笔在线支付 = 多条流水的多笔停车订单」。
 * <p>
 * 当前支持本地联调确认（sandbox）以及微信 / 支付宝异步通知入账。
 */
@Entity
@Table(name = "payment_order")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务支付单号（唯一，对外展示/对账用） */
    @Column(nullable = false, unique = true, length = 32)
    private String payNo;

    /** 车牌号（快照，统一大写） */
    @Column(nullable = false, length = 32)
    private String plateNumber;

    /** 车牌颜色（快照）：下单时选定的颜色，null 表示不限颜色（合并全部颜色记录） */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PlateColor plateColor;

    /** 支付总额（元）= 拆出的各笔停车订单金额之和 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountYuan;

    /** 支付方式：WECHAT_PAY / ALIPAY_PAY */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentMethod method;

    /** 支付单状态：PENDING 待支付 / PAID 已支付 / CLOSED 已关闭 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    /** 是否模拟支付（本地回调）：true 时由用户端调用模拟支付确认接口完成支付 */
    @Column(nullable = false)
    private boolean mock;

    /** 渠道交易号：真实网关回调时写入渠道流水号；模拟支付写入模拟号 */
    @Column(length = 64)
    private String transactionId;

    /** 支付时间：支付成功的时刻 */
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

    public String getPayNo() {
        return payNo;
    }

    public void setPayNo(String payNo) {
        this.payNo = payNo;
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

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentOrderStatus status) {
        this.status = status;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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
