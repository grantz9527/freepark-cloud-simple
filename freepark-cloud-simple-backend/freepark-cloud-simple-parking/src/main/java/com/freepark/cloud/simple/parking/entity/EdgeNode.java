package com.freepark.cloud.simple.parking.entity;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 边缘节点：部署在本地、通过 MQTT 与云端通信的边缘计算单元。
 *
 * <p>每个节点有全局唯一的“节点编号”，作为云端下发配置的目标主题段（{前缀}/{编号}）
 * 与上行心跳的末段（{心跳主题}/{编号}）。一个节点可管辖一个车场（1:1）或同时管辖
 * 多个车场（1:N）；车场与节点的绑定关系通过 ParkingLot.edgeNodeCode 表达，
 * 云端周期把“节点 + 名下车场清单”整体下发，新增/调整绑定无需边缘侧预知。</p>
 */
@Entity
@Table(name = "edge_node")
public class EdgeNode {

    /** 节点编号最大长度（同时受 MQTT 主题段安全约束限制） */
    public static final int MAX_CODE_LENGTH = 64;

    /** 节点名称最大长度 */
    public static final int MAX_NAME_LENGTH = 120;

    /** 备注最大长度 */
    public static final int MAX_REMARK_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 节点编号，全局唯一；创建后不可修改（作为 MQTT 寻址标识） */
    @Column(nullable = false, unique = true, length = MAX_CODE_LENGTH)
    private String code;

    /** 节点名称 */
    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    /** 是否启用：停用后云端不再向其下发配置、也不参与心跳监控 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 备注/位置说明（可选） */
    @Column(length = MAX_REMARK_LENGTH)
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
