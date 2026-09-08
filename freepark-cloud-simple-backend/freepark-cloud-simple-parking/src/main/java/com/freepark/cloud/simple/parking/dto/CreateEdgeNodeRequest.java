package com.freepark.cloud.simple.parking.dto;

/**
 * 创建边缘节点请求。
 *
 * @param name    节点名称（必填）
 * @param enabled 是否启用（缺省 true）
 * @param remark  备注/位置说明（可选）
 *                <p>节点编号由云端按「FreePark + 年月日(站点时区) + 6 位随机数」自动生成，
 *                创建后不可修改，不随请求传入。</p>
 */
public record CreateEdgeNodeRequest(
        String name,
        Boolean enabled,
        String remark) {
}
