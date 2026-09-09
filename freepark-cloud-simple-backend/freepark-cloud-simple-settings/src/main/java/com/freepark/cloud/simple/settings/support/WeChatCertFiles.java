package com.freepark.cloud.simple.settings.support;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;

/**
 * 解析微信商户平台下载的 PEM 证书/密钥文件。
 *
 * <p>商户 API 证书为 X.509 PEM（apiclient_cert.pem / 含公钥）；
 * 私钥为 PKCS#8 PEM（apiclient_key.pem）；微信支付公钥为 PKCS#8 PEM（pub_key.pem）。</p>
 */
public final class WeChatCertFiles {

    private WeChatCertFiles() {
    }

    /**
     * 商户 API 证书解析结果（仅含展示用信息，不含私钥）。
     *
     * @param serialNumber 证书序列号（十六进制大写，无冒号，用于 API v3 请求头）
     * @param issuer       颁发者 DN
     * @param validFrom    生效日期（本地时区）
     * @param validUntil   过期日期（本地时区）
     */
    public record ParsedCert(
            String serialNumber,
            String issuer,
            LocalDate validFrom,
            LocalDate validUntil) {
    }

    /**
     * 解析 PEM 证书文本，提取序列号/颁发者/有效期。
     *
     * @throws IllegalArgumentException PEM 非法或非 X.509 证书
     */
    public static ParsedCert parseCertificate(String pem) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            byte[] der = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))
                    .readAllBytes();
            X509Certificate cert = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(der));
            String serial = HexFormat.of().formatHex(cert.getSerialNumber().toByteArray()).toUpperCase();
            ZoneId zone = ZoneId.systemDefault();
            return new ParsedCert(
                    serial,
                    cert.getIssuerX500Principal().getName(),
                    Instant.ofEpochMilli(cert.getNotBefore().getTime())
                            .atZone(zone).toLocalDate(),
                    Instant.ofEpochMilli(cert.getNotAfter().getTime())
                            .atZone(zone).toLocalDate());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid merchant API certificate PEM", e);
        }
    }

    /**
     * 从原始文件名识别允许上传的证书相关文件（宽松后缀校验）。
     */
    public static boolean isSupportedCertFileName(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".pem")
                || lower.endsWith(".crt")
                || lower.endsWith(".cer")
                || lower.endsWith(".txt");
    }
}
