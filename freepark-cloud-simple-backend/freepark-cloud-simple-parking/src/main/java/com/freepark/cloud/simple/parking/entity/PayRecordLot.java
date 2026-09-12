package com.freepark.cloud.simple.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 支付记录的车场金额明细：一笔支付/退款请求按车场拆开记账，供后续车场维度统计。
 */
@Entity
@Table(name = "pay_record_lot")
public class PayRecordLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recordId;

    private Long lotId;

    @Column(length = 120)
    private String lotName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountYuan;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
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

    public BigDecimal getAmountYuan() {
        return amountYuan;
    }

    public void setAmountYuan(BigDecimal amountYuan) {
        this.amountYuan = amountYuan;
    }
}
