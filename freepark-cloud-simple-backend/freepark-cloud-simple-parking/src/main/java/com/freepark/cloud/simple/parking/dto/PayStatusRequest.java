package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingPayStatus;

/**
 * 人工登记停车流水支付状态请求。
 *
 * @param status 支付状态：UNPAID 未支付 / PARTIAL 部分支付 / PAID 已支付；仅已出场流水可登记
 */
public record PayStatusRequest(ParkingPayStatus status) {
}
