package com.freepark.cloud.simple.parking.entity;

/**
 * 停车订单状态。
 * <ul>
 *   <li>PENDING：待支付（下单后尚未收款，金额计入该流水的「未付订单」预留，避免重复下单）；</li>
 *   <li>PAID：已支付（收款入账，金额累加到关联流水的「累计已支付」）；</li>
 *   <li>PARTIAL_REFUND：部分退款（已支付订单退回一部分，剩余仍计入流水已付）；</li>
 *   <li>REFUNDED：已全额退款（本单已付金额全部退回）；</li>
 *   <li>CANCELLED：已取消（预留释放，不再影响后续下单金额）。</li>
 * </ul>
 */
public enum ParkingOrderStatus {
    PENDING,
    PAID,
    PARTIAL_REFUND,
    REFUNDED,
    CANCELLED
}
