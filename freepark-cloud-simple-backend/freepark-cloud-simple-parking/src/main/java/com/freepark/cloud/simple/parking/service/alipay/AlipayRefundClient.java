package com.freepark.cloud.simple.parking.service.alipay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.settings.dto.AlipayPayRuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝统一收单交易退款（{@code alipay.trade.refund}）。
 */
@Component
public class AlipayRefundClient {

    private static final Logger log = LoggerFactory.getLogger(AlipayRefundClient.class);
    private static final String GATEWAY = "https://openapi.alipay.com/gateway.do";
    private static final String METHOD = "alipay.trade.refund";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AlipayRefundClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 按原商户订单号（及可选支付宝交易号）发起退款。
     *
     * @param outTradeNo   原支付 out_trade_no（本系统缴款单号）
     * @param tradeNo      支付宝交易号（回调 trade_no，可空；有则优先带上）
     * @param outRequestNo 退款请求号（本系统退款单号，同一笔多次退款须唯一）
     * @param refundYuan   本次退款金额（元）
     * @param reason       退款原因（可空）
     */
    public void refund(AlipayPayRuntimeConfig config,
                       String outTradeNo,
                       String tradeNo,
                       String outRequestNo,
                       BigDecimal refundYuan,
                       String reason) {
        if (config == null || !config.readyToPay()
                || !StringUtils.hasText(outTradeNo)
                || !StringUtils.hasText(outRequestNo)) {
            throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_NOT_CONFIGURED);
        }
        String refundAmount = formatAmount(refundYuan);
        if (refundAmount == null) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_REFUND_INVALID_AMOUNT);
        }
        try {
            ObjectNode biz = objectMapper.createObjectNode();
            biz.put("out_trade_no", outTradeNo.trim());
            if (StringUtils.hasText(tradeNo)) {
                biz.put("trade_no", tradeNo.trim());
            }
            biz.put("refund_amount", refundAmount);
            biz.put("out_request_no", outRequestNo.trim());
            if (StringUtils.hasText(reason)) {
                biz.put("refund_reason", truncate(reason.trim(), 256));
            }

            Map<String, String> params = new TreeMap<>();
            params.put("app_id", config.appId().trim());
            params.put("method", METHOD);
            params.put("format", "JSON");
            params.put("charset", "UTF-8");
            params.put("sign_type", "RSA2");
            params.put("timestamp", SiteZoneTimes.toSiteWall(SiteZoneTimes.nowUtc(), SiteZoneTimes.DEFAULT_ZONE)
                    .format(TIMESTAMP));
            params.put("version", "1.0");
            params.put("biz_content", objectMapper.writeValueAsString(biz));
            String signContent = signContent(params);
            params.put("sign", signSha256Rsa(parsePrivateKey(config.appPrivateKey()), signContent));

            String gateway = GATEWAY + "?charset=UTF-8";
            String body = formEncode(params);
            HttpRequest request = HttpRequest.newBuilder(URI.create(gateway))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body() == null ? "{}" : response.body();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resp = root.path("alipay_trade_refund_response");
            if (resp.isMissingNode() || resp.isNull()) {
                resp = root.path("error_response");
            }
            String code = resp.path("code").asText("");
            if ("10000".equals(code)) {
                return;
            }
            String detail = alipayErrorDetail(resp);
            log.warn("支付宝退款失败: outTradeNo={} tradeNo={} outRequestNo={} detail={} body={}",
                    outTradeNo, tradeNo, outRequestNo, detail, responseBody);
            throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_REFUND_FAILED, detail);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("支付宝退款异常 outTradeNo={} outRequestNo={}", outTradeNo, outRequestNo, e);
            String detail = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_REFUND_FAILED, detail);
        }
    }

    private static String alipayErrorDetail(JsonNode resp) {
        if (resp == null || resp.isMissingNode()) {
            return "empty response";
        }
        String subMsg = resp.path("sub_msg").asText("").trim();
        String subCode = resp.path("sub_code").asText("").trim();
        String msg = resp.path("msg").asText("").trim();
        String code = resp.path("code").asText("").trim();
        if (StringUtils.hasText(subMsg) && StringUtils.hasText(subCode)) {
            return subCode + " " + subMsg;
        }
        if (StringUtils.hasText(subMsg)) {
            return subMsg;
        }
        if (StringUtils.hasText(msg) && StringUtils.hasText(code)) {
            return code + " " + msg;
        }
        if (StringUtils.hasText(msg)) {
            return msg;
        }
        return "unknown";
    }

    private static String formatAmount(BigDecimal yuan) {
        if (yuan == null || yuan.signum() <= 0) {
            return null;
        }
        return yuan.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private static String signContent(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!StringUtils.hasText(key) || !StringUtils.hasText(value) || "sign".equals(key)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(value);
        }
        return sb.toString();
    }

    /** 与支付宝开放平台常用编码一致：UTF-8，空格用 %20。 */
    private static String formEncode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(percentEncode(entry.getKey()))
                    .append('=')
                    .append(percentEncode(entry.getValue() == null ? "" : entry.getValue()));
        }
        return sb.toString();
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static PrivateKey parsePrivateKey(String pemOrBase64) throws Exception {
        String normalized = pemOrBase64
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
