package com.freepark.cloud.simple.parking.entity;

/**
 * 停车流水状态。
 */
public enum ParkingSessionStatus {

    /** 在场（进行中） */
    OPEN,

    /** 已出场（正常关闭） */
    CLOSED,

    /** 已作废 */
    VOIDED
}
