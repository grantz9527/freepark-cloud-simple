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

import java.time.LocalDateTime;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 停车场。
 */
@Entity
@Table(name = "parking_lot")
public class ParkingLot {

    /** 通行判定规则顺序串默认值 */
    public static final String DEFAULT_JUDGMENT_ORDER = "BLACKLIST,WHITELIST,PATTERN_ALLOWLIST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 车场名称 */
    @Column(nullable = false, length = 120)
    private String name;

    /** 车场编码，全局唯一 */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 车场类型：INTERNAL 内部车场 / PUBLIC 公共车场 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LotType lotType = LotType.INTERNAL;

    /** 车场地址 */
    @Column(length = 255)
    private String address;

    /** 车位总数 */
    @Column(nullable = false)
    private int totalSpaces = 0;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 入场拦截欠费车辆 */
    @Column(nullable = false)
    private boolean entryInterceptArrears = false;

    /** 入场拦截黑名单车辆 */
    @Column(nullable = false)
    private boolean entryInterceptBlacklist = false;

    /** 入场满位拦截：在场车辆数达到车位总数时禁止入场 */
    @Column(nullable = false)
    private boolean entryInterceptFull = false;

    /** 出场拦截欠费车辆 */
    @Column(nullable = false)
    private boolean exitInterceptArrears = false;

    /** 出场拦截黑名单车辆 */
    @Column(nullable = false)
    private boolean exitInterceptBlacklist = false;

    /**
     * 欠费统计范围：LOT 仅统计本车场，GLOBAL 跨全部车场统计。
     * 供边缘节点「算费请求」按车场维度统计欠费金额时使用。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "arrears_scope", nullable = false, length = 16)
    private LotArrearsScope arrearsScope = LotArrearsScope.LOT;

    /** 通行判定规则顺序（逗号分隔的规则类型枚举名），非法时回退默认顺序 */
    @Column(name = "judgment_order", nullable = false, length = 80)
    private String judgmentOrder = DEFAULT_JUDGMENT_ORDER;

    /** 地图数据 */
    @Column(name = "map_data", columnDefinition = "TEXT")
    private String mapData;

    /**
     * 所属边缘节点编号（可空）：为空表示本车场暂不纳入任何边缘节点管辖。
     * 绑定关系以“车场→节点”的单向编码表达，一个车场最多属于一个节点。
     */
    @Column(name = "edge_node_code", length = 64)
    private String edgeNodeCode;

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

    /** 解析 judgmentOrder 得到生效的通行判定规则顺序，顺序串非法（null/空/缺元素/多余元素）时回退默认顺序 */
    public List<AccessJudgmentRuleType> effectiveJudgmentOrder() {
        if (judgmentOrder == null || judgmentOrder.isBlank()) {
            return AccessJudgmentRuleType.defaultOrder();
        }
        String[] tokens = judgmentOrder.split(",");
        if (tokens.length != AccessJudgmentRuleType.values().length) {
            return AccessJudgmentRuleType.defaultOrder();
        }
        List<AccessJudgmentRuleType> rules = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            try {
                rules.add(AccessJudgmentRuleType.valueOf(token.trim()));
            } catch (IllegalArgumentException ex) {
                return AccessJudgmentRuleType.defaultOrder();
            }
        }
        if (!EnumSet.copyOf(rules).equals(EnumSet.allOf(AccessJudgmentRuleType.class))) {
            return AccessJudgmentRuleType.defaultOrder();
        }
        return List.copyOf(rules);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LotType getLotType() {
        return lotType;
    }

    public void setLotType(LotType lotType) {
        this.lotType = lotType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTotalSpaces() {
        return totalSpaces;
    }

    public void setTotalSpaces(int totalSpaces) {
        this.totalSpaces = totalSpaces;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEntryInterceptArrears() {
        return entryInterceptArrears;
    }

    public void setEntryInterceptArrears(boolean entryInterceptArrears) {
        this.entryInterceptArrears = entryInterceptArrears;
    }

    public boolean isEntryInterceptBlacklist() {
        return entryInterceptBlacklist;
    }

    public void setEntryInterceptBlacklist(boolean entryInterceptBlacklist) {
        this.entryInterceptBlacklist = entryInterceptBlacklist;
    }

    public boolean isEntryInterceptFull() {
        return entryInterceptFull;
    }

    public void setEntryInterceptFull(boolean entryInterceptFull) {
        this.entryInterceptFull = entryInterceptFull;
    }

    public boolean isExitInterceptArrears() {
        return exitInterceptArrears;
    }

    public void setExitInterceptArrears(boolean exitInterceptArrears) {
        this.exitInterceptArrears = exitInterceptArrears;
    }

    public boolean isExitInterceptBlacklist() {
        return exitInterceptBlacklist;
    }

    public void setExitInterceptBlacklist(boolean exitInterceptBlacklist) {
        this.exitInterceptBlacklist = exitInterceptBlacklist;
    }

    public LotArrearsScope getArrearsScope() {
        return arrearsScope;
    }

    public void setArrearsScope(LotArrearsScope arrearsScope) {
        this.arrearsScope = arrearsScope;
    }

    public String getJudgmentOrder() {
        return judgmentOrder;
    }

    public void setJudgmentOrder(String judgmentOrder) {
        this.judgmentOrder = judgmentOrder;
    }

    public String getMapData() {
        return mapData;
    }

    public void setMapData(String mapData) {
        this.mapData = mapData;
    }

    public String getEdgeNodeCode() {
        return edgeNodeCode;
    }

    public void setEdgeNodeCode(String edgeNodeCode) {
        this.edgeNodeCode = edgeNodeCode;
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
