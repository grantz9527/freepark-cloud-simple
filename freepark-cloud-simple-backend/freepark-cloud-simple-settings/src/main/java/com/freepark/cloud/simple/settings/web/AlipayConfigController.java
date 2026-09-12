package com.freepark.cloud.simple.settings.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.pay.PublicOriginResolver;
import com.freepark.cloud.simple.settings.dto.AlipayConfigView;
import com.freepark.cloud.simple.settings.dto.UpdateAlipayConfigUploadRequest;
import com.freepark.cloud.simple.settings.service.AlipayConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 支付宝支付配置：开放平台应用 AppID 与 RSA2 签名密钥（写操作仅超级管理员）。
 */
@RestController
@RequestMapping("/api/system/alipay/config")
public class AlipayConfigController {

    private final AlipayConfigService configService;
    private final PublicOriginResolver publicOrigin;

    public AlipayConfigController(AlipayConfigService configService, PublicOriginResolver publicOrigin) {
        this.configService = configService;
        this.publicOrigin = publicOrigin;
    }

    @GetMapping
    public ApiResult<AlipayConfigView> get(HttpServletRequest request) {
        return ApiResult.ok(configService.getConfig(publicOrigin.alipayNotifyUrl(request)));
    }

    /**
     * 保存支付宝支付配置：应用私钥/支付宝公钥文件随 multipart 提交，
     * 由后端解析并校验密钥格式，无需手工填写密钥内容。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<AlipayConfigView> update(
            @RequestParam(name = "appId", required = false) String appId,
            @RequestParam(name = "notifyUrl", required = false) String notifyUrl,
            @RequestPart(name = "appPrivateKeyFile", required = false) MultipartFile appPrivateKeyFile,
            @RequestPart(name = "alipayPublicKeyFile", required = false) MultipartFile alipayPublicKeyFile,
            HttpServletRequest request) {
        UpdateAlipayConfigUploadRequest body =
                new UpdateAlipayConfigUploadRequest(appId, notifyUrl, appPrivateKeyFile, alipayPublicKeyFile);
        return ApiResult.ok(configService.updateConfig(body, publicOrigin.alipayNotifyUrl(request)));
    }
}
