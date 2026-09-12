package com.freepark.cloud.simple.settings.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * 更新微信支付配置请求（证书上传使用 multipart，其余字段随表单提交）。
 *
 * <p>文件契约：certFile / keyFile 为 null 表示不更新该项；
 * 建议同时上传（商户平台一并下载），两者都提供时会校验公钥/私钥匹配。
 * 上传证书时自动覆盖商户序列号，并解析颁发者/有效期。
 * 是否开放微信支付由「系统配置 → 收费方式」统一控制，本请求不含启停开关。</p>
 */
public record UpdateWeChatConfigUploadRequest(
        String mchId,
        String mchName,
        String mchCertSerialNo,
        String mchApiKey,
        String mpAppId,
        String mpAppSecret,
        String notifyUrl,
        MultipartFile certFile,
        MultipartFile keyFile) {
}
