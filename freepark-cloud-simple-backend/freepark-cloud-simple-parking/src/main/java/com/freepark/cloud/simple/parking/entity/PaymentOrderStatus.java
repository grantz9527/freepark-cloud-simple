package com.freepark.cloud.simple.parking.entity;

/**
 * C 端在线支付单状态。
 * <ul>
 *   <li>PENDING：待支付（已按流水拆出待付停车订单并归集到本支付单）；</li>
 *   <li>PAID：已支付（支付成功，拆出的停车订单全部置为已支付并逐笔入账到对应流水）；</li>
 *   <li>CLOSED：已关闭（用户取消或重新发起支付，释放归集的待付停车订单）。</li>
 * </ul>
 */
public enum PaymentOrderStatus {
    PENDING,
    PAID,
    CLOSED
}
