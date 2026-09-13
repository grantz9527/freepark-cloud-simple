package com.freepark.cloud.simple.parking.service.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.settings.dto.WeChatJsapiRuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 微信申请退款（APIv3 {@code /v3/refund/domestic/refunds}）。
 */
@Component
public class WeChatRefundClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatRefundClient.class);
    private static final String REFUND_PATH = "/v3/refund/domestic/refunds";
    private static final String REFUND_URL = "https://api.mch.weixin.qq.com" + REFUND_PATH;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WeChatRefundClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 按原支付商户单号发起退款。
     *
     * @param outTradeNo  原支付 out_trade_no（本系统缴款单号）
     * @param outRefundNo 商户退款单号（本系统退款单号）
     * @param refundYuan  本次退款金额（元）
     * @param totalYuan   原支付单总金额（元）
     * @param reason      退款原因（可空）
     */
    public void refund(WeChatJsapiRuntimeConfig config,
                       String outTradeNo,
                       String outRefundNo,
                       BigDecimal refundYuan,
                       BigDecimal totalYuan,
                       String reason) {
        if (config == null || !config.ready()
                || !StringUtils.hasText(outTradeNo)
                || !StringUtils.hasText(outRefundNo)) {
            throw new BizException(400, MessageKeys.PAYMENT_WECHAT_NOT_CONFIGURED);
        }
        int refundFen = yuanToFen(refundYuan);
        int totalFen = yuanToFen(totalYuan);
        if (refundFen <= 0 || totalFen <= 0 || refundFen > totalFen) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_REFUND_INVALID_AMOUNT);
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("out_trade_no", outTradeNo.trim());
            body.put("out_refund_no", outRefundNo.trim());
            if (StringUtils.hasText(reason)) {
                body.put("reason", truncate(reason.trim(), 80));
            }
            ObjectNode amount = body.putObject("amount");
            amount.put("refund", refundFen);
            amount.put("total", totalFen);
            amount.put("currency", "CNY");

            String bodyJson = objectMapper.writeValueAsString(body);
            long timestamp = System.currentTimeMillis() / 1000L;
            String nonce = UUID.randomUUID().toString().replace("-", "");
            PrivateKey privateKey = parsePrivateKey(config.privateKeyPem());
            String message = "POST\n" + REFUND_PATH + "\n" + timestamp + "\n" + nonce + "\n" + bodyJson + "\n";
            String signature = signSha256Rsa(privateKey, message);
            String authorization = "WECHATPAY2-SHA256-RSA2048 "
                    + "mchid=\"" + config.mchId() + "\","
                    + "nonce_str=\"" + nonce + "\","
                    + "signature=\"" + signature + "\","
                    + "timestamp=\"" + timestamp + "\","
                    + "serial_no=\"" + config.certSerialNo() + "\"";

            HttpRequest request = HttpRequest.newBuilder(URI.create(REFUND_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", authorization)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body() == null ? "{}" : response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String status = root.path("status").asText("");
                // SUCCESS / PROCESSING 均视为受理成功；到账由微信异步完成
                if ("SUCCESS".equalsIgnoreCase(status)
                        || "PROCESSING".equalsIgnoreCase(status)
                        || !StringUtils.hasText(status)) {
                    return;
                }
                log.warn("微信退款状态异常: status={} body={}", status, response.body());
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_REFUND_FAILED);
            }
            // 同一 out_refund_no 重复提交时微信可能返回已存在类错误码，按成功处理
            String code = root.path("code").asText("");
            if ("INVALID_REQUEST".equals(code) || "FREQUENCY_LIMITED".equals(code)) {
                String messageText = root.path("message").asText("");
                if (messageText.contains("已退款") || messageText.contains("存在")) {
                    log.info("微信退款幂等命中 out_refund_no={}", outRefundNo);
                    return;
                }
            }
            log.warn("微信退款失败: status={} body={}", response.statusCode(), response.body());
            throw new BizException(400, MessageKeys.PAYMENT_WECHAT_REFUND_FAILED);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微信退款异常", e);
            throw new BizException(400, MessageKeys.PAYMENT_WECHAT_REFUND_FAILED);
        }
    }

    private static int yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0;
        }
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static String signSha256Rsa(PrivateKey privateKey, String message) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
