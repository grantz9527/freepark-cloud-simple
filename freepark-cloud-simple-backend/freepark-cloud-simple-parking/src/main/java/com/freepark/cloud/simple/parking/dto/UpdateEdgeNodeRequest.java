package com.freepark.cloud.simple.parking.dto;

/**
 * 更新边缘节点请求（编号不可修改）。
 *
 * @param name    节点名称（必填）
 * @param enabled 是否启用（null=保持原值）
 * @param remark  备注/位置说明（null=保持原值）
 */
public record UpdateEdgeNodeRequest(
        String name,
        Boolean enabled,
        String remark) {
}
