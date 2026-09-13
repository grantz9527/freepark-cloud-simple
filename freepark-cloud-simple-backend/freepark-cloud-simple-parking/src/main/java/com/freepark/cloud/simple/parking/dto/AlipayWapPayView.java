package com.freepark.cloud.simple.parking.dto;

/**
 * 支付宝手机网站支付调起参数：后端签名后的自动提交表单 HTML。
 *
 * @param formHtml 含隐藏域与自动 submit 的表单，前端写入页面后即可跳转收银台
 */
public record AlipayWapPayView(String formHtml) {
}
