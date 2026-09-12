package com.freepark.cloud.simple.settings.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.pay.PaymentNotifyUrls;
import com.freepark.cloud.simple.settings.dto.UpdateWeChatConfigRequest;
import com.freepark.cloud.simple.settings.dto.UpdateWeChatConfigUploadRequest;
import com.freepark.cloud.simple.settings.dto.WeChatConfigView;
import com.freepark.cloud.simple.settings.dto.WeChatJsapiRuntimeConfig;
import com.freepark.cloud.simple.settings.dto.WeChatPayRuntimeConfig;
import com.freepark.cloud.simple.settings.entity.WeChatConfig;
import com.freepark.cloud.simple.settings.repository.WeChatConfigRepository;
import com.freepark.cloud.simple.settings.support.WeChatCertFiles;
import com.freepark.cloud.simple.settings.support.WeChatConfigOptions;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 微信支付配置服务：单例读取/保存，仅超级管理员可修改。
 *
 * <p>密钥契约：各类密钥凭据不回显；保存时留空或 null 表示保持不变。
 * 商户 API 证书（apiclient_cert.pem + apiclient_key.pem）通过文件上传提交，
 * 由后端解析证书提取序列号/颁发者/有效期；商户序列号也可手填。</p>
 */
@Service
public class WeChatConfigService {

    private final WeChatConfigRepository repository;
    private final AdminGuard adminGuard;

    public WeChatConfigService(WeChatConfigRepository repository, AdminGuard adminGuard) {
        this.repository = repository;
        this.adminGuard = adminGuard;
    }

    /**
     * 读取微信支付配置（启用中的管理员可读，页面按菜单角色限制为超管）。
     *
     * @param defaultNotifyUrl 当前环境拼接出的默认支付结果通知地址
     */
    @Transactional(readOnly = true)
    public WeChatConfigView getConfig(String defaultNotifyUrl) {
        adminGuard.requireEnabledAdmin();
        return toView(requireConfig(), defaultNotifyUrl);
    }

    /**
     * 当前生效的支付回调地址（自定义优先，否则系统默认）。供下单 notify_url 使用。
     */
    @Transactional(readOnly = true)
    public String effectiveNotifyUrl(String defaultNotifyUrl) {
        return PaymentNotifyUrls.resolve(
                repository.findById(WeChatConfig.SINGLETON_ID).map(WeChatConfig::getNotifyUrl).orElse(""),
                defaultNotifyUrl);
    }

    /**
     * 公众号 AppID（非密钥）。供 C 端在普通浏览器里走网页授权以拉起微信。
     */
    @Transactional(readOnly = true)
    public String publicMpAppId() {
        return repository.findById(WeChatConfig.SINGLETON_ID)
                .map(WeChatConfig::getMpAppId)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .orElse("");
    }

    /**
     * 支付回调验签用的运行时凭据（无登录，仅供渠道通知处理读取）。
     */
    @Transactional(readOnly = true)
    public WeChatPayRuntimeConfig loadRuntime() {
        return repository.findById(WeChatConfig.SINGLETON_ID)
                .map(config -> new WeChatPayRuntimeConfig(
                        blankToEmpty(config.getMchId()),
                        blankToEmpty(config.getMchApiKey()),
                        blankToEmpty(config.getWechatPayPublicKey()),
                        blankToEmpty(config.getWechatPayPublicKeyId())))
                .orElseGet(WeChatPayRuntimeConfig::empty);
    }

    /**
     * JSAPI 下单与 OAuth 换 openid 用的商户/公众号凭据（无登录，仅供缴费链路读取）。
     */
    @Transactional(readOnly = true)
    public WeChatJsapiRuntimeConfig loadJsapiRuntime() {
        return repository.findById(WeChatConfig.SINGLETON_ID)
                .map(config -> new WeChatJsapiRuntimeConfig(
                        blankToEmpty(config.getMchId()),
                        blankToEmpty(config.getMchApiKey()),
                        blankToEmpty(config.getMpAppId()),
                        blankToEmpty(config.getMpAppSecret()),
                        blankToEmpty(config.getMchPrivateKeyPem()),
                        blankToEmpty(config.getMchCertSerialNo()),
                        blankToEmpty(config.getNotifyUrl())))
                .orElseGet(WeChatJsapiRuntimeConfig::empty);
    }

    /**
     * 保存基础文本字段（商户号/名称/序列号/密钥/公众号，仅超级管理员）。
     */
    @Transactional
    public WeChatConfigView updateConfig(UpdateWeChatConfigRequest request, String defaultNotifyUrl) {
        requireSuperAdmin();
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        WeChatConfig config = requireConfig();
        applyTextFields(config, request.mchId(), request.mchName(), request.mchCertSerialNo(),
                request.mchApiKey(), request.mpAppId(), request.mpAppSecret(),
                request.notifyUrl(), defaultNotifyUrl);
        return toView(repository.saveAndFlush(config), defaultNotifyUrl);
    }

    /**
     * 保存含证书上传的完整配置（仅超级管理员，multipart 提交）。
     * 提供证书文件时自动解析序列号/颁发者/有效期，并做私钥-证书公钥匹配校验。
     */
    @Transactional
    public WeChatConfigView updateConfigWithUpload(UpdateWeChatConfigUploadRequest request, String defaultNotifyUrl) {
        requireSuperAdmin();
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        WeChatConfig config = requireConfig();
        applyTextFields(config, request.mchId(), request.mchName(), request.mchCertSerialNo(),
                request.mchApiKey(), request.mpAppId(), request.mpAppSecret(),
                request.notifyUrl(), defaultNotifyUrl);
        applyUploadedFiles(config, request);
        return toView(repository.saveAndFlush(config), defaultNotifyUrl);
    }

    private void applyTextFields(WeChatConfig config, String mchId, String mchName, String mchCertSerialNo,
                                 String mchApiKey, String mpAppId, String mpAppSecret,
                                 String notifyUrl, String defaultNotifyUrl) {
        String normalizedMchId = WeChatConfigOptions.validateMchId(mchId);
        String normalizedMchName = WeChatConfigOptions.validateMchName(mchName);
        String normalizedSerial = WeChatConfigOptions.validateCertSerialNo(mchCertSerialNo);
        String normalizedAppId = WeChatConfigOptions.validateMpAppId(mpAppId);
        String normalizedApiKey = WeChatConfigOptions.validateMchApiKey(mchApiKey);
        String normalizedAppSecret = WeChatConfigOptions.validateMpAppSecret(mpAppSecret);
        String normalizedNotify = PaymentNotifyUrls.normalizeForStore(notifyUrl, defaultNotifyUrl);
        config.setMchId(normalizedMchId);
        config.setMchName(normalizedMchName);
        config.setMchCertSerialNo(normalizedSerial);
        config.setMpAppId(normalizedAppId);
        config.setNotifyUrl(normalizedNotify);
        // 各类密钥留空表示保持不变
        if (normalizedApiKey != null) {
            config.setMchApiKey(normalizedApiKey);
        }
        if (normalizedAppSecret != null) {
            config.setMpAppSecret(normalizedAppSecret);
        }
    }

    /**
     * 处理 multipart 上传的证书/私钥文件。
     * certFile 与 keyFile 均提供时解析证书并做私钥-证书匹配校验；二者都不提供则跳过本段。
     * 上传成功时覆盖商户序列号及颁发者/有效期。
     */
    private void applyUploadedFiles(WeChatConfig config, UpdateWeChatConfigUploadRequest request) {
        MultipartFile certFile = request.certFile();
        MultipartFile keyFile = request.keyFile();

        boolean hasCertFile = certFile != null && !certFile.isEmpty();
        boolean hasKeyFile = keyFile != null && !keyFile.isEmpty();
        if (hasCertFile || hasKeyFile) {
            if (!(hasCertFile && hasKeyFile)) {
                // 证书与私钥配套下载，须成对提交
                throw new BizException(400, MessageKeys.WECHAT_CERT_AND_KEY_TOGETHER);
            }
            String certPem = readFileText(certFile);
            String keyPem = readFileText(keyFile);
            WeChatCertFiles.ParsedCert parsed;
            try {
                parsed = WeChatCertFiles.parseCertificate(certPem);
            } catch (IllegalArgumentException e) {
                throw new BizException(400, MessageKeys.WECHAT_CERT_PEM_INVALID);
            }
            verifyKeyMatchesCert(keyPem, certPem);
            config.setMchCertPem(certPem);
            config.setMchPrivateKeyPem(keyPem);
            config.setMchCertSerialNo(parsed.serialNumber());
            config.setMchCertIssuer(parsed.issuer());
            config.setMchCertValidUntil(parsed.validUntil());
        }
    }

    private String readFileText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException(400, MessageKeys.WECHAT_CERT_READ_FAILED);
        }
    }

    /**
     * 私钥与证书匹配校验：二者应是同一张商户 API 证书。
     * 通过 PKCS#8 解析私钥得到 RSA 模数，与证书公钥模数比较，一致才视为配套。
     */
    private void verifyKeyMatchesCert(String keyPem, String certPem) {
        try {
            PublicKey certPublicKey = certificatePublicKey(certPem);
            if (!(certPublicKey instanceof RSAPublicKey rsaPublicKey)) {
                throw new BizException(400, MessageKeys.WECHAT_CERT_PEM_INVALID);
            }
            BigInteger certModulus = rsaPublicKey.getModulus();
            BigInteger keyModulus = privateKeyModulus(keyPem);
            if (!certModulus.equals(keyModulus)) {
                throw new BizException(400, MessageKeys.WECHAT_CERT_KEY_MISMATCH);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(400, MessageKeys.WECHAT_CERT_PEM_INVALID);
        }
    }

    private PublicKey certificatePublicKey(String certPem) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
        return cert.getPublicKey();
    }

    /**
     * 解析 PKCS#8 RSA 私钥文本并返回其模数（用于与证书公钥比对）。
     *
     * @throws IllegalArgumentException PEM 头尾非法、内容非 PKCS#8 RSA 私钥
     */
    private BigInteger privateKeyModulus(String keyPem) throws Exception {
        String body = keyPem
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(body);
        PrivateKey key = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
        if (key instanceof RSAPrivateCrtKey crtKey) {
            return crtKey.getModulus();
        }
        throw new IllegalArgumentException("Not an RSA private key");
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
    private WeChatConfig requireConfig() {
        return repository.findById(WeChatConfig.SINGLETON_ID)
                .orElseGet(() -> repository.save(WeChatConfig.defaults()));
    }

    private WeChatConfigView toView(WeChatConfig config, String defaultNotifyUrl) {
        String defaults = defaultNotifyUrl == null ? "" : defaultNotifyUrl.trim();
        return new WeChatConfigView(
                config.getMchId(),
                config.getMchName(),
                !isBlank(config.getMchApiKey()),
                !isBlank(config.getMchPrivateKeyPem()),
                config.getMchCertSerialNo(),
                config.getMchCertIssuer(),
                config.getMchCertValidUntil(),
                config.getMpAppId(),
                !isBlank(config.getMpAppSecret()),
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
