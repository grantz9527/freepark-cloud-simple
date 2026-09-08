package com.freepark.cloud.simple.parking.entity;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 优惠车辆（指定车辆每次入场免费时长）记录。
 * <p>
 * 同一车场下同一车牌只允许一条记录：该车在本车场每次入场，自入场时刻起
 * {@link #freeMinutes} 分钟内免收停车费，超出部分按车场当前生效的计费模板正常计费。
 * 仅云端结算生效（不影响通行判定 / 名单下发），停用或删除后不再享受优惠。
 * </p>
 */
@Entity
@Table(name = "discount_vehicle")
public class DiscountVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属车场 */
    @ManyToOne(optional = false)
    private ParkingLot lot;

    /** 车牌号（同一车场唯一） */
    @Column(nullable = false, length = 20)
    private String plateNumber;

    /** 车牌颜色 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlateColor plateColor = PlateColor.BLUE;

    /** 每次入场的免费时长（分钟，1 ~ 10080） */
    @Column(nullable = false)
    private Integer freeMinutes;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 备注 */
    @Column(length = 255)
    private String remark;

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

    public ParkingLot getLot() {
        return lot;
    }

    public void setLot(ParkingLot lot) {
        this.lot = lot;
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

    public Integer getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(Integer freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
