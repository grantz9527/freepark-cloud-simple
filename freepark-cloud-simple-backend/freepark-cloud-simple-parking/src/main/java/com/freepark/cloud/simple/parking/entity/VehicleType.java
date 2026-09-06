package com.freepark.cloud.simple.parking.entity;

/**
 * 车辆类型。
 */
public enum VehicleType {

    /** 临时车 */
    TEMPORARY,

    /** 预约车 */
    RESERVED,

    /** VIP 车辆 */
    VIP,

    /** 业主车辆 */
    OWNER,

    /** 月租车辆 */
    MONTHLY,

    /** 其他 */
    OTHER
}
