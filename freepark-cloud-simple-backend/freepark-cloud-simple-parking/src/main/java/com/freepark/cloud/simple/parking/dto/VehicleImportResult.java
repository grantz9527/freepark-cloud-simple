package com.freepark.cloud.simple.parking.dto;

/**
 * Excel 导入结果：内部车辆导入返回批次号（用于批量删除本次导入），
 * 白名单/黑名单导入无批次号（batchId 为 null）。
 */
public record VehicleImportResult(String batchId, int imported, int skipped) {
}
