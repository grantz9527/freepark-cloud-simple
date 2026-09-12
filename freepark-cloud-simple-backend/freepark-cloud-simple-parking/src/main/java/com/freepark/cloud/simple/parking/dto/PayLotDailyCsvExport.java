package com.freepark.cloud.simple.parking.dto;

/**
 * 车场按日统计 CSV 导出。
 */
public record PayLotDailyCsvExport(byte[] content, String filename) {
}
