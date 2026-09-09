package com.freepark.cloud.simple.settings.entity;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 支付宝支付配置（单例）：云端收款所需的支付宝开放平台应用参数与签名密钥。
 *
 * <p>是否开放支付宝支付由「系统配置 → 收费方式」统一控制，本配置只保存凭据，
 * 不持有启停开关。</p>
 *
 * <p>字段用途：</p>
 * <ul>
 *   <li>应用 AppID：支付宝开放平台应用的唯一标识，同时用于用户授权与收单</li>
 *   <li>应用私钥：支付请求签名使用（RSA2），由文件上传解析</li>
 *   <li>支付宝公钥：校验支付宝响应与异步通知的签名（公钥模式）</li>
 * </ul>
 *
 * <p>密钥契约：密钥不回显；保存时留空或为 null 表示保持不变。</p>
 */
@Entity
@Table(name = "alipay_config")
public class AlipayConfig {

    /** 单例记录主键 */
    public static final String SINGLETON_ID = "default";

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id = SINGLETON_ID;

    /** 支付宝开放平台应用 AppID（纯数字） */
    @Column(name = "app_id", nullable = false, length = 32)
    private String appId = "";

    /** 应用私钥文本（RSA2，PKCS#8 PEM；敏感，响应中不回显，由文件上传解析） */
    @Column(name = "app_private_key_pem", columnDefinition = "TEXT")
    private String appPrivateKeyPem;

    /** 支付宝公钥内容（RSA 验签用；敏感，响应中不回显，由文件上传解析） */
    @Column(name = "alipay_public_key", columnDefinition = "TEXT")
    private String alipayPublicKey;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected AlipayConfig() {
    }

    /**
     * 供配置服务懒创建默认行（凭据为空，由后续保存动作填充）。
     */
    public static AlipayConfig defaults() {
        return new AlipayConfig();
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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppPrivateKeyPem() {
        return appPrivateKeyPem;
    }

    public void setAppPrivateKeyPem(String appPrivateKeyPem) {
        this.appPrivateKeyPem = appPrivateKeyPem;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
