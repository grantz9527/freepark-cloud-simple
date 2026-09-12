package com.freepark.cloud.simple.settings.entity;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 微信支付配置（单例）：云端收款所需的微信商户与授权公众号参数。
 *
 * <p>是否开放微信支付由「系统配置 → 收费方式」统一控制，本配置只保存凭据，
 * 不持有启停开关。</p>
 *
 * <p>字段用途：</p>
 * <ul>
 *   <li>商户号/商户名称/商户序列号/商户 API 密钥：微信支付收单（API 调用签名）</li>
 *   <li>商户 API 证书：请求签名；上传后自动覆盖商户序列号</li>
 *   <li>公众号 AppID/AppSecret：用户缴费前的微信授权登录（OAuth）</li>
 * </ul>
 *
 * <p>密钥契约：密钥不回显；保存时留空或为 null 表示保持不变。</p>
 */
@Entity
@Table(name = "wechat_config")
public class WeChatConfig {

    /** 单例记录主键 */
    public static final String SINGLETON_ID = "default";

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id = SINGLETON_ID;

    /**
     * 历史列：启停已迁到「系统配置 → 收费方式」。
     * 旧库 ddl-auto=update 不会删列；保留映射并默认 false，避免首次插入报
     * 「Field 'enabled' doesn't have a default value」。
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /** 微信支付商户号（纯数字） */
    @Column(name = "mch_id", nullable = false, length = 32)
    private String mchId = "";

    /** 商户名称（展示/对账用，可与商户平台登记名称一致） */
    @Column(name = "mch_name", length = 128)
    private String mchName = "";

    /** 商户 API 密钥：API v3 密钥（32 位字母数字；敏感，响应中不回显） */
    @Column(name = "mch_api_key", length = 64)
    private String mchApiKey;

    /** 商户 API 证书私钥文本（apiclient_key.pem；敏感，响应中不回显，由文件上传解析） */
    @Column(name = "mch_private_key_pem", columnDefinition = "TEXT")
    private String mchPrivateKeyPem;

    /** 商户 API 证书文本（apiclient_cert.pem，用于匹配校验与 SDK 参考；不回显） */
    @Column(name = "mch_cert_pem", columnDefinition = "TEXT")
    private String mchCertPem;

    /** 商户序列号（商户 API 证书序列号；可手填，上传证书时自动覆盖） */
    @Column(name = "mch_cert_serial_no", length = 64)
    private String mchCertSerialNo = "";

    /** 商户 API 证书颁发者（解析信息，展示用） */
    @Column(name = "mch_cert_issuer", length = 255)
    private String mchCertIssuer;

    /** 商户 API 证书有效期截止（解析信息，展示用；以服务器时区本地时间保存） */
    @Column(name = "mch_cert_valid_until")
    private LocalDate mchCertValidUntil;

    /**
     * 历史列：微信支付公钥模式已不在配置页采集。
     * 旧库保留列以免 ddl-auto 丢数据；新流程不再读写。
     */
    @Column(name = "wechat_pay_public_key_id", length = 64)
    private String wechatPayPublicKeyId;

    /** 历史列：同上，回调若仍存有公钥可继续验签。 */
    @Column(name = "wechat_pay_public_key", columnDefinition = "TEXT")
    private String wechatPayPublicKey;

    /** 缴费授权公众号 AppID（形如 wx + 16 位） */
    @Column(name = "mp_app_id", nullable = false, length = 32)
    private String mpAppId = "";

    /** 缴费授权公众号 AppSecret（敏感，响应中不回显） */
    @Column(name = "mp_app_secret", length = 128)
    private String mpAppSecret;

    /**
     * 支付回调地址覆盖：空表示使用系统默认（后台基础地址 + 固定路径）。
     */
    @Column(name = "notify_url", length = 512)
    private String notifyUrl = "";

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected WeChatConfig() {
    }

    /**
     * 供配置服务懒创建默认行（凭据为空，由后续保存动作填充）。
     */
    public static WeChatConfig defaults() {
        return new WeChatConfig();
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

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId;
    }

    public String getMchName() {
        return mchName == null ? "" : mchName;
    }

    public void setMchName(String mchName) {
        this.mchName = mchName == null ? "" : mchName;
    }

    public String getMchApiKey() {
        return mchApiKey;
    }

    public void setMchApiKey(String mchApiKey) {
        this.mchApiKey = mchApiKey;
    }

    public String getMchCertPem() {
        return mchCertPem;
    }

    public void setMchCertPem(String mchCertPem) {
        this.mchCertPem = mchCertPem;
    }

    public String getMchPrivateKeyPem() {
        return mchPrivateKeyPem;
    }

    public void setMchPrivateKeyPem(String mchPrivateKeyPem) {
        this.mchPrivateKeyPem = mchPrivateKeyPem;
    }

    public String getMchCertSerialNo() {
        return mchCertSerialNo == null ? "" : mchCertSerialNo;
    }

    public void setMchCertSerialNo(String mchCertSerialNo) {
        this.mchCertSerialNo = mchCertSerialNo == null ? "" : mchCertSerialNo;
    }

    public String getMchCertIssuer() {
        return mchCertIssuer;
    }

    public void setMchCertIssuer(String mchCertIssuer) {
        this.mchCertIssuer = mchCertIssuer;
    }

    public LocalDate getMchCertValidUntil() {
        return mchCertValidUntil;
    }

    public void setMchCertValidUntil(LocalDate mchCertValidUntil) {
        this.mchCertValidUntil = mchCertValidUntil;
    }

    public String getWechatPayPublicKeyId() {
        return wechatPayPublicKeyId;
    }

    public void setWechatPayPublicKeyId(String wechatPayPublicKeyId) {
        this.wechatPayPublicKeyId = wechatPayPublicKeyId;
    }

    public String getWechatPayPublicKey() {
        return wechatPayPublicKey;
    }

    public void setWechatPayPublicKey(String wechatPayPublicKey) {
        this.wechatPayPublicKey = wechatPayPublicKey;
    }

    public String getMpAppId() {
        return mpAppId;
    }

    public void setMpAppId(String mpAppId) {
        this.mpAppId = mpAppId;
    }

    public String getMpAppSecret() {
        return mpAppSecret;
    }

    public void setMpAppSecret(String mpAppSecret) {
        this.mpAppSecret = mpAppSecret;
    }

    public String getNotifyUrl() {
        return notifyUrl == null ? "" : notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl == null ? "" : notifyUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
