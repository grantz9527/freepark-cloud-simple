package com.freepark.cloud.simple.settings.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.settings.dto.UpdateWeChatConfigRequest;
import com.freepark.cloud.simple.settings.dto.UpdateWeChatConfigUploadRequest;
import com.freepark.cloud.simple.settings.dto.WeChatConfigView;
import com.freepark.cloud.simple.settings.service.WeChatConfigService;
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
 * 微信支付配置：商户号/商户密钥/授权公众号参数（写操作仅超级管理员）。
 */
@RestController
@RequestMapping("/api/system/wechat/config")
public class WeChatConfigController {

    private final WeChatConfigService configService;

    public WeChatConfigController(WeChatConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ApiResult<WeChatConfigView> get() {
        return ApiResult.ok(configService.getConfig());
    }

    @PutMapping
    public ApiResult<WeChatConfigView> update(@RequestBody UpdateWeChatConfigRequest request) {
        return ApiResult.ok(configService.updateConfig(request));
    }

    /**
     * 保存含证书上传的配置：证书/私钥/微信支付公钥文件随 multipart 提交，
     * 证书序列号/颁发者/有效期由后端解析提取，无需手工填写。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<WeChatConfigView> updateWithUpload(
            @RequestParam(name = "mchId", required = false) String mchId,
            @RequestParam(name = "mchApiKey", required = false) String mchApiKey,
            @RequestParam(name = "mpAppId", required = false) String mpAppId,
            @RequestParam(name = "mpAppSecret", required = false) String mpAppSecret,
            @RequestParam(name = "wechatPayPublicKeyId", required = false) String wechatPayPublicKeyId,
            @RequestPart(name = "certFile", required = false) MultipartFile certFile,
            @RequestPart(name = "keyFile", required = false) MultipartFile keyFile,
            @RequestPart(name = "wechatPayPublicKeyFile", required = false) MultipartFile wechatPayPublicKeyFile) {
        UpdateWeChatConfigUploadRequest request = new UpdateWeChatConfigUploadRequest(
                mchId, mchApiKey, mpAppId, mpAppSecret, wechatPayPublicKeyId,
                certFile, keyFile, wechatPayPublicKeyFile);
        return ApiResult.ok(configService.updateConfigWithUpload(request));
    }
}
