package com.freepark.cloud.simple.settings.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.pay.PublicOriginResolver;
import com.freepark.cloud.simple.settings.dto.UpdateWeChatConfigUploadRequest;
import com.freepark.cloud.simple.settings.dto.UpdateWeChatConfigRequest;
import com.freepark.cloud.simple.settings.dto.WeChatConfigView;
import com.freepark.cloud.simple.settings.service.WeChatConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 微信支付配置：商户号/名称/序列号/商户密钥/授权公众号参数（写操作仅超级管理员）。
 */
@RestController
@RequestMapping("/api/system/wechat/config")
public class WeChatConfigController {

    private final WeChatConfigService configService;
    private final PublicOriginResolver publicOrigin;

    public WeChatConfigController(WeChatConfigService configService, PublicOriginResolver publicOrigin) {
        this.configService = configService;
        this.publicOrigin = publicOrigin;
    }

    @GetMapping
    public ApiResult<WeChatConfigView> get(HttpServletRequest request) {
        return ApiResult.ok(configService.getConfig(publicOrigin.wechatNotifyUrl(request)));
    }

    @PutMapping
    public ApiResult<WeChatConfigView> update(@RequestBody UpdateWeChatConfigRequest body,
                                              HttpServletRequest request) {
        return ApiResult.ok(configService.updateConfig(body, publicOrigin.wechatNotifyUrl(request)));
    }

    /**
     * 保存含证书上传的配置：证书/私钥文件随 multipart 提交，
     * 上传时自动覆盖商户序列号并解析颁发者/有效期。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<WeChatConfigView> updateWithUpload(
            @RequestParam(name = "mchId", required = false) String mchId,
            @RequestParam(name = "mchName", required = false) String mchName,
            @RequestParam(name = "mchCertSerialNo", required = false) String mchCertSerialNo,
            @RequestParam(name = "mchApiKey", required = false) String mchApiKey,
            @RequestParam(name = "mpAppId", required = false) String mpAppId,
            @RequestParam(name = "mpAppSecret", required = false) String mpAppSecret,
            @RequestParam(name = "notifyUrl", required = false) String notifyUrl,
            @RequestPart(name = "certFile", required = false) MultipartFile certFile,
            @RequestPart(name = "keyFile", required = false) MultipartFile keyFile,
            HttpServletRequest request) {
        UpdateWeChatConfigUploadRequest body = new UpdateWeChatConfigUploadRequest(
                mchId, mchName, mchCertSerialNo, mchApiKey, mpAppId, mpAppSecret, notifyUrl, certFile, keyFile);
        return ApiResult.ok(configService.updateConfigWithUpload(body, publicOrigin.wechatNotifyUrl(request)));
    }
}
