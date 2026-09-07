package com.freepark.cloud.simple.parking.entity;

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
import com.freepark.cloud.simple.common.time.SiteZoneTimes;

/**
 * 停车流水：入场识别自动生成在场流水，出场识别匹配并关闭流水。
 * 关联的车场/通道/识别记录均以快照字段回指（平铺存储，无实体关联）。
 */
@Entity
@Table(name = "parking_session")
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 车场 ID（快照） */
    private Long lotId;

    /** 车场名称（快照） */
    @Column(length = 120)
    private String lotName;

    /** 车牌号 */
    @Column(nullable = false, length = 32)
    private String plateNumber;

    /** 车牌颜色 */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PlateColor plateColor;

    /** 流水状态：OPEN 在场 / CLOSED 已出场 / VOIDED 已作废 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ParkingSessionStatus status = ParkingSessionStatus.OPEN;

    /** 入场时间 */
    @Column(nullable = false)
    private LocalDateTime entryTime;

    /** 入场通道 ID（快照） */
    private Long entryLaneId;

    /** 入场通道名称（快照） */
    @Column(length = 120)
    private String entryLaneName;

    /** 入场识别记录 ID（快照） */
    private Long entryRecognitionId;

    /** 入场抓拍图片 */
    @Column(columnDefinition = "TEXT")
    private String entryImage;

    /** 出场时间 */
    private LocalDateTime exitTime;

    /** 出场通道 ID（快照） */
    private Long exitLaneId;

    /** 出场通道名称（快照） */
    @Column(length = 120)
    private String exitLaneName;

    /** 出场识别记录 ID（快照） */
    private Long exitRecognitionId;

    /** 出场抓拍图片 */
    @Column(columnDefinition = "TEXT")
    private String exitImage;

    /**
     * 应收金额快照（元）：仅在新增加场关联的写事件（入场/出入场更新）或手动「重新算费」时，
     * 按「车场 × 车牌颜色 × 入场日期」生效的计费绑定结算并落库；未计费为 null，列表查询不重算。
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal feeYuan;

    /**
     * 支付状态：仅已出场（CLOSED）流水登记；在场/已作废为 null。
     * 关场时默认置为 UNPAID，之后由人工登记更新，不随费用重算自动变化。
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ParkingPayStatus payStatus;

    /** 支付时间：登记为「已支付」的时刻；仅已支付流水有意义。 */
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

    /** 出场匹配成功：关闭流水。默认支付状态为未支付（若此前未登记）。 */
    public void closeWithExit(LocalDateTime exitTime, Long laneId, String laneName,
                              Long recognitionId, String image) {
        this.status = ParkingSessionStatus.CLOSED;
        this.exitTime = exitTime;
        this.exitLaneId = laneId;
        this.exitLaneName = laneName;
        this.exitRecognitionId = recognitionId;
        this.exitImage = image;
        if (this.payStatus == null) {
            this.payStatus = ParkingPayStatus.UNPAID;
        }
    }

    /** 作废流水（在场或已出场均可作废）。作废后支付状态与时间一并清空。 */
    public void markVoided() {
        this.status = ParkingSessionStatus.VOIDED;
        this.payStatus = null;
        this.payTime = null;
    }

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

    public ParkingSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingSessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public Long getEntryLaneId() {
        return entryLaneId;
    }

    public void setEntryLaneId(Long entryLaneId) {
        this.entryLaneId = entryLaneId;
    }

    public String getEntryLaneName() {
        return entryLaneName;
    }

    public void setEntryLaneName(String entryLaneName) {
        this.entryLaneName = entryLaneName;
    }

    public Long getEntryRecognitionId() {
        return entryRecognitionId;
    }

    public void setEntryRecognitionId(Long entryRecognitionId) {
        this.entryRecognitionId = entryRecognitionId;
    }

    public String getEntryImage() {
        return entryImage;
    }

    public void setEntryImage(String entryImage) {
        this.entryImage = entryImage;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public Long getExitLaneId() {
        return exitLaneId;
    }

    public void setExitLaneId(Long exitLaneId) {
        this.exitLaneId = exitLaneId;
    }

    public String getExitLaneName() {
        return exitLaneName;
    }

    public void setExitLaneName(String exitLaneName) {
        this.exitLaneName = exitLaneName;
    }

    public Long getExitRecognitionId() {
        return exitRecognitionId;
    }

    public void setExitRecognitionId(Long exitRecognitionId) {
        this.exitRecognitionId = exitRecognitionId;
    }

    public String getExitImage() {
        return exitImage;
    }

    public void setExitImage(String exitImage) {
        this.exitImage = exitImage;
    }

    public BigDecimal getFeeYuan() {
        return feeYuan;
    }

    public void setFeeYuan(BigDecimal feeYuan) {
        this.feeYuan = feeYuan;
    }

    public ParkingPayStatus getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(ParkingPayStatus payStatus) {
        this.payStatus = payStatus;
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
