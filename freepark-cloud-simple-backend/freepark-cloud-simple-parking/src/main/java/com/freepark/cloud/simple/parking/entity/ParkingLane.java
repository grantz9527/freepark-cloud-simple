package com.freepark.cloud.simple.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;

/**
 * 通道（出入口）。
 */
@Entity
@Table(name = "parking_lane")
public class ParkingLane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属车场 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id")
    private ParkingLot lot;

    /** 关联（对向）车场：双向通道通往的车场，可空 */
    @ManyToOne
    @JoinColumn(name = "linked_lot_id")
    private ParkingLot linkedLot;

    /** 通道名称 */
    @Column(nullable = false, length = 120)
    private String name;

    /** 通道编码，全局唯一 */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 通道类型：ENTRANCE 入口 / EXIT 出口 / BIDIRECTIONAL 双向 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LaneType laneType = LaneType.ENTRANCE;

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

    public ParkingLot getLinkedLot() {
        return linkedLot;
    }

    public void setLinkedLot(ParkingLot linkedLot) {
        this.linkedLot = linkedLot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LaneType getLaneType() {
        return laneType;
    }

    public void setLaneType(LaneType laneType) {
        this.laneType = laneType;
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
