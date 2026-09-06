package com.freepark.cloud.simple.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import java.util.ArrayList;
import java.util.List;

/**
 * 岗亭。
 */
@Entity
@Table(name = "parking_booth")
public class ParkingBooth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属车场 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id")
    private ParkingLot lot;

    /** 岗亭名称 */
    @Column(nullable = false, length = 120)
    private String name;

    /** 岗亭编码 */
    @Column(length = 64)
    private String code;

    /** 岗亭位置 */
    @Column(length = 255)
    private String location;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 关联通道 */
    @ManyToMany
    @JoinTable(name = "parking_booth_lane",
            joinColumns = @JoinColumn(name = "booth_id"),
            inverseJoinColumns = @JoinColumn(name = "lane_id"))
    private List<ParkingLane> lanes = new ArrayList<>();

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ParkingLane> getLanes() {
        return lanes;
    }

    public void setLanes(List<ParkingLane> lanes) {
        this.lanes = lanes;
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
