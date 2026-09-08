package com.freepark.cloud.simple.settings.runtime;

/**
 * 边缘配置快照构建器：针对某个车场构建同步负载 data.lots 数组中的单个元素（JSON 字符串）。
 *
 * <p>接口定义在 settings（不感知具体业务实体）；由提供车场数据的业务模块
 * （如 parking）提供面向实际业务数据（如车场自身的开关配置、通行判定顺序）的实现。
 * 调度方按“边缘节点 → 名下车场清单”为每个车场调用本构建器，聚合为节点快照下发。</p>
 */
public interface EdgeConfigSnapshotBuilder {

    /**
     * 构建单个车场配置快照的 JSON 片段。
     *
     * @param parkCode 车场编码（来自边缘节点名下 EdgeSyncTarget.parkCodes()）
     * @return 合法 JSON 字符串；无法构建（如车场已删除/不适用）时返回 null，调度方跳过该车场
     */
    String buildConfigDataJson(String parkCode);
}
