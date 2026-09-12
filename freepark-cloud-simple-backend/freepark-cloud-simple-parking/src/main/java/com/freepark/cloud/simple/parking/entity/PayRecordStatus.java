package com.freepark.cloud.simple.parking.entity;

/**
 * 支付记录状态（支付请求与退款请求共用）。
 * <ul>
 *   <li>PENDING：已向平台发起、等待结果；</li>
 *   <li>SUCCESS：平台确认成功；</li>
 *   <li>CLOSED：关闭或失败（用户取消、渠道失败、重新下单）。</li>
 * </ul>
 */
public enum PayRecordStatus {
    PENDING,
    SUCCESS,
    CLOSED
}
