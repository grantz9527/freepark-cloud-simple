package com.freepark.cloud.simple.settings.dto;

/**
 * 更新微信支付配置的基础文本字段请求。
 *
 * <p>密钥契约：mchApiKey / mpAppSecret 为 null 或空串表示保持不变
 * （无法清空）。商户 API 证书与微信支付公钥走
 * {@link UpdateWeChatConfigUploadRequest} 文件上传接口。
 * 是否开放微信支付由「系统配置 → 收费方式」统一控制，本请求不含启停开关。</p>
 *
 * @param mchId        微信支付商户号（纯数字，必填）
 * @param mchApiKey    商户 API 密钥（可选更新，仅字母数字）
 * @param mpAppId      缴费授权公众号 AppID（形如 wx + 16 位，必填）
 * @param mpAppSecret  公众号 AppSecret（可选更新）
 */
public record UpdateWeChatConfigRequest(
        String mchId,
        String mchApiKey,
        String mpAppId,
        String mpAppSecret) {
}
