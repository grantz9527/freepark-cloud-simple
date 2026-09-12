package com.freepark.cloud.simple.parking.event;

import com.freepark.cloud.simple.parking.entity.PlateColor;

/**
 * C 端缴款单入账成功（事务提交后由 MQTT 开闸发布器消费）。
 *
 * @param payNo        缴款单号
 * @param plateNumber  车牌（已规范化大写）
 * @param plateColor   下单时选定的颜色；null 表示不限颜色
 */
public record PaymentSettledEvent(String payNo, String plateNumber, PlateColor plateColor) {
}
