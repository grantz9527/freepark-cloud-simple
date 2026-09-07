package com.freepark.cloud.simple.parking.entity;

/**
 * 停车流水支付状态：仅在已出场（CLOSED）流水上登记；在场/已作废流水无支付状态。
 * 由人工登记支付结果，不随费用重算自动变化。
 */
public enum ParkingPayStatus {
    /** 未支付 */
    UNPAID,
    /** 部分支付 */
    PARTIAL,
    /** 已支付（全部付清） */
    PAID
}
