package com.freepark.cloud.simple.parking.entity;

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
import com.freepark.cloud.simple.common.time.SiteZoneTimes;

/**
 * 内部车辆。
 */
@Entity
@Table(name = "internal_vehicle")
public class InternalVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属车场 */
    @ManyToOne(optional = false)
    private ParkingLot lot;

    /** 车牌号 */
    @Column(nullable = false, length = 20)
    private String plateNumber;

    /** 车牌颜色 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlateColor plateColor = PlateColor.BLUE;

    /** 车主姓名 */
    @Column(nullable = false, length = 80)
    private String ownerName;

    /** 车辆类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VehicleType type = VehicleType.OTHER;

    /** 联系电话 */
    @Column(length = 32)
    private String phone;

    /** 所属部门 */
    @Column(length = 80)
    private String department;

    /** 备注 */
    @Column(length = 255)
    private String remark;

    /** 批量导入批次号 */
    @Column(length = 36)
    private String batchId;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
