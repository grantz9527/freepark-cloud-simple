package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车费查询（单车欠费流水）结果。
 *
 * @param list        命中「欠费」条件的停车流水分页数据（已出场、应收金额 &gt; 0 且未付清）
 * @param total       命中总条数（未分页全量）
 * @param page        当前页码
 * @param size        每页条数
 * @param totalAmount 该车全部欠费流水合计应收（元），无命中为 0
 */
public record VehicleArrearsResult(
        List<ParkingSessionView> list,
        long total,
        int page,
        int size,
        BigDecimal totalAmount) {
}
