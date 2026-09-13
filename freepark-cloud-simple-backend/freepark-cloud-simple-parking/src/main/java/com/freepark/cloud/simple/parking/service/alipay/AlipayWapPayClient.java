package com.freepark.cloud.simple.parking.service.alipay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.parking.dto.AlipayWapPayView;
import com.freepark.cloud.simple.settings.dto.AlipayPayRuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝手机网站支付（{@code alipay.trade.wap.pay}）：本地 RSA2 签名并生成跳转表单。
 */
@Component
public class AlipayWapPayClient {

    private static final Logger log = LoggerFactory.getLogger(AlipayWapPayClient.class);
    private static final String GATEWAY = "https://openapi.alipay.com/gateway.do";
    private static final String METHOD = "alipay.trade.wap.pay";
    private static final String PRODUCT_CODE = "QUICK_WAP_WAY";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    public AlipayWapPayClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 生成可直接提交的支付表单 HTML（pageExecute 等价实现）。
     *
     * @param notifyUrl 异步通知地址（须公网可达）
     * @param returnUrl 同步跳回用户端缴款结果页
     */
    public AlipayWapPayView pagePay(AlipayPayRuntimeConfig config,
                                   String payNo,
                                   String subject,
                                   BigDecimal amountYuan,
                                   String notifyUrl,
                                   String returnUrl) {
        if (config == null || !config.readyToPay()
                || !StringUtils.hasText(payNo)
                || !StringUtils.hasText(notifyUrl)
                || !StringUtils.hasText(returnUrl)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        String totalAmount = formatAmount(amountYuan);
        if (totalAmount == null) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_NOT_PAYABLE);
        }
        try {
            ObjectNode biz = objectMapper.createObjectNode();
            biz.put("out_trade_no", payNo.trim());
            biz.put("total_amount", totalAmount);
            biz.put("subject", truncate(subject, 256));
            biz.put("product_code", PRODUCT_CODE);
            biz.put("quit_url", returnUrl);

            Map<String, String> params = new TreeMap<>();
            params.put("app_id", config.appId().trim());
            params.put("method", METHOD);
            params.put("format", "JSON");
            params.put("charset", "utf-8");
            params.put("sign_type", "RSA2");
            params.put("timestamp", SiteZoneTimes.toSiteWall(SiteZoneTimes.nowUtc(), SiteZoneTimes.DEFAULT_ZONE)
                    .format(TIMESTAMP));
            params.put("version", "1.0");
            params.put("notify_url", notifyUrl.trim());
            params.put("return_url", returnUrl.trim());
            params.put("biz_content", objectMapper.writeValueAsString(biz));

            String content = signContent(params);
            PrivateKey privateKey = parsePrivateKey(config.appPrivateKey());
            params.put("sign", signSha256Rsa(privateKey, content));

            return new AlipayWapPayView(buildAutoSubmitForm(params));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("支付宝手机网站支付下单异常", e);
            throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_ORDER_FAILED);
        }
    }

    private static String formatAmount(BigDecimal yuan) {
        if (yuan == null || yuan.signum() <= 0) {
            return null;
        }
        return yuan.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String truncate(String text, int maxChars) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return "停车费";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
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

    private static String buildAutoSubmitForm(Map<String, String> params) {
        String action = GATEWAY + "?charset=" + URLEncoder.encode("utf-8", StandardCharsets.UTF_8);
        StringBuilder html = new StringBuilder(1024);
        html.append("<form id=\"alipaysubmit\" name=\"alipaysubmit\" action=\"")
                .append(escapeAttr(action))
                .append("\" method=\"POST\">");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("<input type=\"hidden\" name=\"")
                    .append(escapeAttr(entry.getKey()))
                    .append("\" value=\"")
                    .append(escapeAttr(entry.getValue()))
                    .append("\"/>");
        }
        html.append("<input type=\"submit\" value=\"ok\" style=\"display:none;\"/>")
                .append("</form>")
                .append("<script>document.forms['alipaysubmit'].submit();</script>");
        return html.toString();
    }

    private static String escapeAttr(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
