package com.freepark.cloud.simple.parking.entity;

/**
 * 支付记录类型：每次向支付平台发起的请求记一行。
 * <ul>
 *   <li>PAY：支付请求；</li>
 *   <li>REFUND：退款请求。</li>
 * </ul>
 */
public enum PayRecordKind {
    PAY,
    REFUND
}
