package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;

/**
 * 算费结果：欠费金额（元）。未欠费为 0，车场不存在/不可识别时同样返回 0（不拦截）。
 */
public record EdgeArrearsQuoteResponse(BigDecimal amount) {
}
