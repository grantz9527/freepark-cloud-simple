package com.freepark.cloud.simple.parking.service.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.parking.dto.WeChatJsapiPayView;
import com.freepark.cloud.simple.settings.dto.WeChatJsapiRuntimeConfig;
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
import java.util.Base64;
import java.util.UUID;

/**
 * 微信网页授权换 openid + JSAPI 统一下单，并生成前端调起支付签名。
 */
@Component
public class WeChatJsapiPayClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatJsapiPayClient.class);
    private static final String OAUTH_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String JSAPI_PREPAY_PATH = "/v3/pay/transactions/jsapi";
    private static final String JSAPI_PREPAY_URL = "https://api.mch.weixin.qq.com" + JSAPI_PREPAY_PATH;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WeChatJsapiPayClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 用 snsapi_base 授权 code 换取付款用户 openid。 */
    public String exchangeOpenId(WeChatJsapiRuntimeConfig config, String wxCode) {
        if (!StringUtils.hasText(wxCode)) {
            throw new BizException(400, MessageKeys.PAYMENT_WECHAT_OAUTH_REQUIRED);
        }
        try {
            String url = OAUTH_TOKEN_URL
                    + "?appid=" + urlEncode(config.mpAppId())
                    + "&secret=" + urlEncode(config.mpAppSecret())
                    + "&code=" + urlEncode(wxCode.trim())
                    + "&grant_type=authorization_code";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            if (root.hasNonNull("errcode") && root.path("errcode").asInt() != 0) {
                log.warn("微信 OAuth 换 openid 失败: errcode={} errmsg={}",
                        root.path("errcode").asText(), root.path("errmsg").asText());
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_OAUTH_FAILED);
            }
            String openid = root.path("openid").asText("");
            if (!StringUtils.hasText(openid)) {
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_OAUTH_FAILED);
            }
            return openid;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微信 OAuth 换 openid 异常", e);
            throw new BizException(400, MessageKeys.PAYMENT_WECHAT_OAUTH_FAILED);
        }
    }

    /**
     * JSAPI 下单并返回前端调起参数。
     *
     * @param notifyUrl 支付结果异步通知地址（须公网可达）
     */
    public WeChatJsapiPayView prepay(WeChatJsapiRuntimeConfig config,
                                    String payNo,
                                    String description,
                                    String attach,
                                    BigDecimal amountYuan,
                                    String openid,
                                    String notifyUrl) {
        if (!StringUtils.hasText(openid) || !StringUtils.hasText(notifyUrl) || !StringUtils.hasText(payNo)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        int totalFen = yuanToFen(amountYuan);
        if (totalFen <= 0) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_NOT_PAYABLE);
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("appid", config.mpAppId());
            body.put("mchid", config.mchId());
            body.put("description", truncate(description, 127));
            body.put("out_trade_no", payNo);
            body.put("notify_url", notifyUrl);
            if (StringUtils.hasText(attach)) {
                body.put("attach", truncate(attach.trim(), 127));
            }
            ObjectNode amount = body.putObject("amount");
            amount.put("total", totalFen);
            amount.put("currency", "CNY");
            body.putObject("payer").put("openid", openid);

            String bodyJson = objectMapper.writeValueAsString(body);
            long timestamp = System.currentTimeMillis() / 1000L;
            String nonce = UUID.randomUUID().toString().replace("-", "");
            PrivateKey privateKey = parsePrivateKey(config.privateKeyPem());
            String message = "POST\n" + JSAPI_PREPAY_PATH + "\n" + timestamp + "\n" + nonce + "\n" + bodyJson + "\n";
            String signature = signSha256Rsa(privateKey, message);
            String authorization = "WECHATPAY2-SHA256-RSA2048 "
                    + "mchid=\"" + config.mchId() + "\","
                    + "nonce_str=\"" + nonce + "\","
                    + "signature=\"" + signature + "\","
                    + "timestamp=\"" + timestamp + "\","
                    + "serial_no=\"" + config.certSerialNo() + "\"";

            HttpRequest request = HttpRequest.newBuilder(URI.create(JSAPI_PREPAY_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", authorization)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body() == null ? "{}" : response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("微信 JSAPI 下单失败: status={} body={}", response.statusCode(), response.body());
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_ORDER_FAILED);
            }
            String prepayId = root.path("prepay_id").asText("");
            if (!StringUtils.hasText(prepayId)) {
                log.warn("微信 JSAPI 下单无 prepay_id: {}", response.body());
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_ORDER_FAILED);
            }
            return buildJsapiParams(config.mpAppId(), prepayId, privateKey);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微信 JSAPI 下单异常", e);
            throw new BizException(400, MessageKeys.PAYMENT_WECHAT_ORDER_FAILED);
        }
    }

    private WeChatJsapiPayView buildJsapiParams(String appId, String prepayId, PrivateKey privateKey) throws Exception {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String packageValue = "prepay_id=" + prepayId;
        String message = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";
        String paySign = signSha256Rsa(privateKey, message);
        return new WeChatJsapiPayView(appId, timeStamp, nonceStr, packageValue, "RSA", paySign);
    }

    private static int yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0;
        }
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static String truncate(String text, int maxChars) {
        String value = text == null ? "" : text.trim();
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
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
