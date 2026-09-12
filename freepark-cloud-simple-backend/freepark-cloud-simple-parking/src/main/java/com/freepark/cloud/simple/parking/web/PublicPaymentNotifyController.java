package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.parking.service.PaymentNotifyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信 / 支付宝支付结果异步通知（免登录）。应答格式按渠道要求，不走统一 ApiResult。
 */
@RestController
@RequestMapping("/api/public/payment")
public class PublicPaymentNotifyController {

    private static final Map<String, String> WECHAT_OK = Map.of("code", "SUCCESS", "message", "成功");
    private static final Map<String, String> WECHAT_FAIL = Map.of("code", "FAIL", "message", "失败");

    private final PaymentNotifyService notifyService;

    public PublicPaymentNotifyController(PaymentNotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /** 微信支付 APIv3 支付结果通知。必须读原始 JSON 报文体做验签，不能走 Jackson 反序列化。 */
    @PostMapping(value = "/wechat/notify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> wechatNotify(HttpServletRequest request) {
        try {
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean ok = notifyService.handleWechat(
                    request.getHeader("Wechatpay-Timestamp"),
                    request.getHeader("Wechatpay-Nonce"),
                    request.getHeader("Wechatpay-Signature"),
                    request.getHeader("Wechatpay-Serial"),
                    body);
            return ResponseEntity.ok(ok ? WECHAT_OK : WECHAT_FAIL);
        } catch (Exception e) {
            return ResponseEntity.ok(WECHAT_FAIL);
        }
    }

    /** 支付宝异步通知：应答纯文本 success / fail。 */
    @PostMapping(value = "/alipay/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> alipayNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0 && values[0] != null) {
                    params.put(key, values[0]);
                }
            });
            boolean ok = notifyService.handleAlipay(params);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(ok ? "success" : "fail");
        } catch (Exception e) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("fail");
        }
    }
}
