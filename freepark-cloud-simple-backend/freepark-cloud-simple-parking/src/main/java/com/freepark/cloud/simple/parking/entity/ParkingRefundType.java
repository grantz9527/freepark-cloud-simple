package com.freepark.cloud.simple.parking.entity;

/**
 * 单笔退款类型。
 * <ul>
 *   <li>PARTIAL：本次退款后订单仍有剩余可退；</li>
 *   <li>FULL：本次退款后本单已全部退完。</li>
 * </ul>
 */
public enum ParkingRefundType {
    PARTIAL,
    FULL
}
