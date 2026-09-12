package com.freepark.cloud.simple.settings.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * 更新支付宝支付配置请求（文件上传使用 multipart，其余字段随表单提交）。
 *
 * <p>文件契约：appPrivateKeyFile / alipayPublicKeyFile 为 null 表示不更新该项；
 * 上传时后端会自动校验私钥为合法 RSA2（PKCS#8 PEM）、公钥为合法 RSA 公钥内容。
 * 是否开放支付宝支付由「系统配置 → 收费方式」统一控制，本请求不含启停开关。</p>
 *
 * @param appId               支付宝开放平台应用 AppID（纯数字，可空）
 * @param notifyUrl           支付回调地址（可空或等于默认则跟随系统默认）
 * @param appPrivateKeyFile   应用私钥文件（RSA2，支付宝密钥工具导出的 PEM）
 * @param alipayPublicKeyFile 支付宝公钥内容文件（开放平台密钥页复制保存，或 PEM 文本）
 */
public record UpdateAlipayConfigUploadRequest(
        String appId,
        String notifyUrl,
        MultipartFile appPrivateKeyFile,
        MultipartFile alipayPublicKeyFile) {
}
