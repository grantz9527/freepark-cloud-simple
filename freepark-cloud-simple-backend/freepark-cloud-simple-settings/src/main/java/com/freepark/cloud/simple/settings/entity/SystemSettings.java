package com.freepark.cloud.simple.settings.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局站点配置（单例，参考 freepark local_server 的 SiteSettings）。
 *
 * <p>目前承载“区域与语言”（默认语言/时区）、
 * “后台基础地址”（支付回调拼接）、“用户端基础地址”（用户端首页/缴费页）、
 * “车牌颜色”（默认颜色/允许颜色）、
 * “收费金额单位”（默认币种/允许币种）、“收费方式”（允许的缴费渠道）
 * 与“用户端缴费范围”（强制全部支付 / 允许勾选指定订单），
 * 其余参考配置项暂未接入。</p>
 */
@Entity
@Table(name = "system_settings")
public class SystemSettings {

    /** 单例记录主键 */
    public static final String SINGLETON_ID = "default";

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id = SINGLETON_ID;

    /** 默认语言（语言标签，如 zh-CN / en） */
    @Column(nullable = false, length = 16)
    private String defaultLocale;

    /** 默认时区（IANA 时区，如 Asia/Shanghai） */
    @Column(nullable = false, length = 64)
    private String timezone;

    /** 站点默认车牌版式（区域），历史记录缺失时按 CN 兜底 */
    @Column(name = "plate_region", nullable = true, length = 16)
    private String plateRegion;

    /** 默认车牌颜色（PlateColor 名称，与 parking 模块枚举保持一致） */
    @Column(name = "default_plate_color", nullable = false, length = 32)
    private String defaultPlateColor;

    /** 允许的车牌颜色集合（有序） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "system_settings_allowed_plate_color",
            joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "plate_color", nullable = false, length = 32)
    @OrderColumn(name = "sort_order")
    private List<String> allowedPlateColors = new ArrayList<>();

    /** 默认币种（ISO 4217 货币代码，如 CNY）：站点收费金额的统一展示/录入单位 */
    @Column(name = "default_currency", nullable = false, length = 16)
    private String defaultCurrency;

    /** 允许使用的币种集合（有序，自预置货币列表勾选启用） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "system_settings_allowed_currency",
            joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "currency", nullable = false, length = 16)
    @OrderColumn(name = "sort_order")
    private List<String> allowedCurrencies = new ArrayList<>();

    /** 允许使用的收费方式集合（有序，自预置方式列表勾选启用） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "system_settings_allowed_payment_method",
            joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "payment_method", nullable = false, length = 32)
    @OrderColumn(name = "sort_order")
    private List<String> allowedPaymentMethods = new ArrayList<>();

    /**
     * 后台基础地址：云端 API 公网根（如 https://cloud.example.com 或 https://host/fangzhi），无尾斜杠。
     * 用于拼接微信/支付宝支付回调 URL；未填写时按当前请求 Host 推断。
     * 列名沿用 site_base_url，避免既有库丢数据。
     */
    @Column(name = "site_base_url", length = 255)
    private String adminBaseUrl = "";

    /**
     * 用户端基础地址：C 端网页公网根（如 https://pay.example.com 或 https://host/freepark-user），无尾斜杠。
     * 用于用户端首页、缴费结果页与渠道同步跳回；未填写时回落后台基础地址。
     */
    @Column(name = "user_base_url", length = 255)
    private String userBaseUrl = "";

    /**
     * 用户端缴费范围：true 强制一次缴清该车牌全部欠费；
     * false 允许勾选指定停车记录缴费。空值按 true 兜底。
     */
    @Column(name = "force_pay_all")
    private Boolean forcePayAll = Boolean.TRUE;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected SystemSettings() {
    }

    public SystemSettings(String defaultLocale, String timezone, String plateRegion,
                          String defaultPlateColor, List<String> allowedPlateColors,
                          String defaultCurrency, List<String> allowedCurrencies,
                          List<String> allowedPaymentMethods) {
        this.defaultLocale = defaultLocale;
        this.timezone = timezone;
        this.plateRegion = plateRegion;
        this.defaultPlateColor = defaultPlateColor;
        this.allowedPlateColors = new ArrayList<>(allowedPlateColors);
        this.defaultCurrency = defaultCurrency;
        this.allowedCurrencies = new ArrayList<>(allowedCurrencies);
        this.allowedPaymentMethods = new ArrayList<>(allowedPaymentMethods);
    }

    @PrePersist
    void prePersist() {
        this.updatedAt = SiteZoneTimes.nowUtc();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = SiteZoneTimes.nowUtc();
    }

    public String getId() {
        return id;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getPlateRegion() {
        return plateRegion;
    }

    public void setPlateRegion(String plateRegion) {
        this.plateRegion = plateRegion;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDefaultPlateColor() {
        return defaultPlateColor;
    }

    public void setDefaultPlateColor(String defaultPlateColor) {
        this.defaultPlateColor = defaultPlateColor;
    }

    public List<String> getAllowedPlateColors() {
        return allowedPlateColors;
    }

    public void setAllowedPlateColors(List<String> allowedPlateColors) {
        this.allowedPlateColors = new ArrayList<>(allowedPlateColors);
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public List<String> getAllowedCurrencies() {
        return allowedCurrencies;
    }

    public void setAllowedCurrencies(List<String> allowedCurrencies) {
        this.allowedCurrencies = new ArrayList<>(allowedCurrencies);
    }

    public List<String> getAllowedPaymentMethods() {
        return allowedPaymentMethods;
    }

    public void setAllowedPaymentMethods(List<String> allowedPaymentMethods) {
        this.allowedPaymentMethods = new ArrayList<>(allowedPaymentMethods);
    }

    public String getAdminBaseUrl() {
        return adminBaseUrl == null ? "" : adminBaseUrl;
    }

    public void setAdminBaseUrl(String adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl == null ? "" : adminBaseUrl;
    }

    public String getUserBaseUrl() {
        return userBaseUrl == null ? "" : userBaseUrl;
    }

    public void setUserBaseUrl(String userBaseUrl) {
        this.userBaseUrl = userBaseUrl == null ? "" : userBaseUrl;
    }

    public boolean isForcePayAll() {
        return forcePayAll == null || forcePayAll;
    }

    public void setForcePayAll(Boolean forcePayAll) {
        this.forcePayAll = forcePayAll == null ? Boolean.TRUE : forcePayAll;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
