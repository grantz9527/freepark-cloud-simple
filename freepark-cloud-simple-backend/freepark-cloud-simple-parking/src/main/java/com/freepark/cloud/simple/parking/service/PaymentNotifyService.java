package com.freepark.cloud.simple.parking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepark.cloud.simple.parking.entity.PaymentMethod;
import com.freepark.cloud.simple.settings.dto.AlipayPayRuntimeConfig;
import com.freepark.cloud.simple.settings.dto.WeChatPayRuntimeConfig;
import com.freepark.cloud.simple.settings.service.AlipayConfigService;
import com.freepark.cloud.simple.settings.service.WeChatConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 微信 / 支付宝异步通知：验签、解密后把缴款单入账。
 * 渠道要求的应答格式由控制器输出；本服务只返回是否应回复成功。
 */
@Service
public class PaymentNotifyService {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotifyService.class);
    private static final long WECHAT_TIMESTAMP_SKEW_SECONDS = 300;
    private static final int GCM_TAG_BITS = 128;

    private final WeChatConfigService weChatConfig;
    private final AlipayConfigService alipayConfig;
    private final PublicPaymentService payments;
    private final ObjectMapper objectMapper;

    public PaymentNotifyService(WeChatConfigService weChatConfig,
                                AlipayConfigService alipayConfig,
                                PublicPaymentService payments,
                                ObjectMapper objectMapper) {
        this.weChatConfig = weChatConfig;
        this.alipayConfig = alipayConfig;
        this.payments = payments;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理微信支付 APIv3 通知。验签失败或业务拒绝返回 false（渠道会重试）。
     */
    public boolean handleWechat(String timestamp, String nonce, String signature,
                                String serial, String body) {
        WeChatPayRuntimeConfig config = weChatConfig.loadRuntime();
        if (!config.ready()) {
            log.warn("微信支付回调：商户号或 APIv3 密钥未配置");
            return false;
        }
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature) || body == null) {
            log.warn("微信支付回调：缺少签名头或报文");
            return false;
        }
        if (!withinSkew(timestamp)) {
            log.warn("微信支付回调：时间戳超出允许范围");
            return false;
        }
        try {
            if (config.hasPlatformPublicKey()) {
                if (StringUtils.hasText(config.wechatPayPublicKeyId())
                        && StringUtils.hasText(serial)
                        && !config.wechatPayPublicKeyId().equalsIgnoreCase(serial.trim())) {
                    log.warn("微信支付回调：公钥 ID 与 Wechatpay-Serial 不一致");
                    return false;
                }
                PublicKey publicKey = parseRsaPublicKey(config.wechatPayPublicKeyPem());
                String message = timestamp + "\n" + nonce + "\n" + body + "\n";
                if (!verifySha256Rsa(publicKey, message, signature)) {
                    log.warn("微信支付回调：签名校验失败");
                    return false;
                }
            } else {
                log.warn("微信支付回调：未配置平台公钥，跳过签名校验（仅依赖 APIv3 解密）");
            }
            JsonNode root = objectMapper.readTree(body);
            String eventType = root.path("event_type").asText("");
            if (!"TRANSACTION.SUCCESS".equalsIgnoreCase(eventType)) {
                return true;
            }
            JsonNode resource = root.path("resource");
            String plaintext = decryptWechatResource(
                    config.apiV3Key(),
                    resource.path("associated_data").asText(""),
                    resource.path("nonce").asText(""),
                    resource.path("ciphertext").asText(""));
            JsonNode payload = objectMapper.readTree(plaintext);
            String tradeState = payload.path("trade_state").asText("");
            if (!"SUCCESS".equalsIgnoreCase(tradeState)) {
                return true;
            }
            String mchId = payload.path("mchid").asText("");
            if (StringUtils.hasText(mchId) && !mchId.equals(config.mchId())) {
                log.warn("微信支付回调：商户号不匹配");
                return false;
            }
            String payNo = payload.path("out_trade_no").asText("");
            String transactionId = payload.path("transaction_id").asText("");
            BigDecimal amountYuan = fenToYuan(payload.path("amount").path("total").asLong(-1));
            ChannelSettleResult result = payments.applyChannelSuccess(
                    payNo, transactionId, PaymentMethod.WECHAT_PAY, amountYuan);
            if (result == ChannelSettleResult.REJECT) {
                log.warn("微信支付回调：入账拒绝 payNo={}", payNo);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("微信支付回调处理失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 处理支付宝异步通知。验签失败或业务拒绝返回 false。
     */
    public boolean handleAlipay(Map<String, String> params) {
        AlipayPayRuntimeConfig config = alipayConfig.loadRuntime();
        if (!config.ready()) {
            log.warn("支付宝回调：AppID 或支付宝公钥未配置");
            return false;
        }
        if (params == null || params.isEmpty()) {
            return false;
        }
        String sign = params.get("sign");
        if (!StringUtils.hasText(sign)) {
            log.warn("支付宝回调：缺少 sign");
            return false;
        }
        try {
            PublicKey publicKey = parseRsaPublicKey(config.alipayPublicKey());
            String content = alipaySignContent(params);
            if (!verifySha256Rsa(publicKey, content, sign)) {
                log.warn("支付宝回调：签名校验失败");
                return false;
            }
            String appId = params.get("app_id");
            if (StringUtils.hasText(appId) && !appId.equals(config.appId())) {
                log.warn("支付宝回调：AppID 不匹配");
                return false;
            }
            String tradeStatus = params.getOrDefault("trade_status", "");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return true;
            }
            String payNo = params.get("out_trade_no");
            String transactionId = params.get("trade_no");
            BigDecimal amountYuan = null;
            String totalAmount = params.get("total_amount");
            if (StringUtils.hasText(totalAmount)) {
                amountYuan = new BigDecimal(totalAmount.trim());
            }
            ChannelSettleResult result = payments.applyChannelSuccess(
                    payNo, transactionId, PaymentMethod.ALIPAY_PAY, amountYuan);
            if (result == ChannelSettleResult.REJECT) {
                log.warn("支付宝回调：入账拒绝 payNo={}", payNo);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("支付宝回调处理失败：{}", e.getMessage());
            return false;
        }
    }

    private static boolean withinSkew(String timestamp) {
        try {
            long seconds = Long.parseLong(timestamp.trim());
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - seconds) <= WECHAT_TIMESTAMP_SKEW_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String decryptWechatResource(String apiV3Key, String associatedData,
                                                String nonce, String ciphertext) throws Exception {
        byte[] key = apiV3Key.getBytes(StandardCharsets.UTF_8);
        if (key.length != 32) {
            throw new IllegalArgumentException("APIv3 key must be 32 bytes");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce.getBytes(StandardCharsets.UTF_8)));
        if (StringUtils.hasText(associatedData)) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
    }

    private static BigDecimal fenToYuan(long fen) {
        if (fen < 0) {
            return null;
        }
        return BigDecimal.valueOf(fen).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String alipaySignContent(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder content = new StringBuilder();
        for (String key : keys) {
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            String value = params.get(key);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (content.length() > 0) {
                content.append('&');
            }
            content.append(key).append('=').append(value);
        }
        return content.toString();
    }

    private static boolean verifySha256Rsa(PublicKey publicKey, String content, String signatureBase64)
            throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(content.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(signatureBase64.replaceAll("\\s", "")));
    }

    private static PublicKey parseRsaPublicKey(String pemOrBase64) throws Exception {
        String body = pemOrBase64.trim();
        body = body.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(body);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }
}
