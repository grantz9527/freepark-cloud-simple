package com.freepark.cloud.simple.settings.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.pay.PaymentNotifyUrls;
import com.freepark.cloud.simple.settings.dto.AlipayConfigView;
import com.freepark.cloud.simple.settings.dto.AlipayPayRuntimeConfig;
import com.freepark.cloud.simple.settings.dto.UpdateAlipayConfigUploadRequest;
import com.freepark.cloud.simple.settings.entity.AlipayConfig;
import com.freepark.cloud.simple.settings.repository.AlipayConfigRepository;
import com.freepark.cloud.simple.settings.support.AlipayConfigOptions;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 支付宝支付配置服务：单例读取/保存，仅超级管理员可修改。
 *
 * <p>密钥契约：各类密钥凭据不回显；保存时留空或 null 表示保持不变。
 * 应用私钥（RSA2，PKCS#8；支持密钥工具导出的裸 Base64 .txt 或 PEM）与支付宝公钥
 * 通过文件上传接口提交，由后端解析并校验密钥格式后保存。</p>
 */
@Service
public class AlipayConfigService {

    private final AlipayConfigRepository repository;
    private final AdminGuard adminGuard;

    public AlipayConfigService(AlipayConfigRepository repository, AdminGuard adminGuard) {
        this.repository = repository;
        this.adminGuard = adminGuard;
    }

    /**
     * 读取支付宝支付配置（启用中的管理员可读，页面按菜单角色限制为超管）。
     *
     * @param defaultNotifyUrl 当前环境拼接出的默认异步通知地址
     */
    @Transactional(readOnly = true)
    public AlipayConfigView getConfig(String defaultNotifyUrl) {
        adminGuard.requireEnabledAdmin();
        return toView(requireConfig(), defaultNotifyUrl);
    }

    /**
     * 当前生效的支付回调地址（自定义优先，否则系统默认）。供下单 notify_url 使用。
     */
    @Transactional(readOnly = true)
    public String effectiveNotifyUrl(String defaultNotifyUrl) {
        return PaymentNotifyUrls.resolve(
                repository.findById(AlipayConfig.SINGLETON_ID).map(AlipayConfig::getNotifyUrl).orElse(""),
                defaultNotifyUrl);
    }

    /**
     * 支付宝运行时凭据（无登录）：下单签名与异步通知验签共用。
     */
    @Transactional(readOnly = true)
    public AlipayPayRuntimeConfig loadRuntime() {
        return repository.findById(AlipayConfig.SINGLETON_ID)
                .map(config -> new AlipayPayRuntimeConfig(
                        blankToEmpty(config.getAppId()),
                        blankToEmpty(config.getAppPrivateKeyPem()),
                        blankToEmpty(config.getAlipayPublicKey())))
                .orElseGet(AlipayPayRuntimeConfig::empty);
    }

    /**
     * 保存支付宝支付配置（仅超级管理员，multipart 提交）。
     * 提供文件时自动校验密钥格式；不提供的密钥项保持原值。
     */
    @Transactional
    public AlipayConfigView updateConfig(UpdateAlipayConfigUploadRequest request, String defaultNotifyUrl) {
        requireSuperAdmin();
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        AlipayConfig config = requireConfig();
        config.setAppId(AlipayConfigOptions.validateAppId(request.appId()));
        config.setNotifyUrl(PaymentNotifyUrls.normalizeForStore(request.notifyUrl(), defaultNotifyUrl));
        applyUploadedFiles(config, request);
        return toView(repository.saveAndFlush(config), defaultNotifyUrl);
    }

    /**
     * 处理 multipart 上传的应用私钥与支付宝公钥文件；二者都未提供则仅更新应用 AppID。
     */
    private void applyUploadedFiles(AlipayConfig config, UpdateAlipayConfigUploadRequest request) {
        MultipartFile privateKeyFile = request.appPrivateKeyFile();
        MultipartFile publicKeyFile = request.alipayPublicKeyFile();
        if (privateKeyFile != null && !privateKeyFile.isEmpty()) {
            String privateKey = AlipayConfigOptions.validateAppPrivateKeyPem(readFileText(privateKeyFile));
            if (privateKey != null) {
                config.setAppPrivateKeyPem(privateKey);
            }
        }
        if (publicKeyFile != null && !publicKeyFile.isEmpty()) {
            String publicKey = AlipayConfigOptions.validateAlipayPublicKey(readFileText(publicKeyFile));
            if (publicKey != null) {
                config.setAlipayPublicKey(publicKey);
            }
        }
    }

    private String readFileText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException(400, MessageKeys.ALIPAY_KEY_READ_FAILED);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 返回单例配置；若记录缺失则懒创建默认行。
     */
    private AlipayConfig requireConfig() {
        return repository.findById(AlipayConfig.SINGLETON_ID)
                .orElseGet(() -> repository.save(AlipayConfig.defaults()));
    }

    private AlipayConfigView toView(AlipayConfig config, String defaultNotifyUrl) {
        String defaults = defaultNotifyUrl == null ? "" : defaultNotifyUrl.trim();
        return new AlipayConfigView(
                config.getAppId(),
                !isBlank(config.getAppPrivateKeyPem()),
                !isBlank(config.getAlipayPublicKey()),
                config.getUpdatedAt(),
                PaymentNotifyUrls.resolve(config.getNotifyUrl(), defaults),
                defaults);
    }

    private void requireSuperAdmin() {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        String role = operator.getRole();
        if (!UserAccount.ROLE_SUPER_ADMIN.equals(role)) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
    }
}
