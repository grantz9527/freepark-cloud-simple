package com.freepark.cloud.simple.settings.runtime;

/**
 * 边缘配置快照构建器：针对某个车场构建同步负载中的 data 片段（JSON 字符串）。
 *
 * <p>接口定义在 settings（不感知具体业务实体）；由提供车场数据的业务模块
 * （如 parking）提供面向实际业务数据（如车场自身的开关配置、通行判定顺序）的实现。</p>
 */
public interface EdgeConfigSnapshotBuilder {

    /**
     * 构建车场配置快照的 data 片段 JSON。
     *
     * @param parkCode 车场编码（来自 EdgeSyncTarget）
     * @return 合法 JSON 字符串；无法构建（如车场已删除/不适用）时返回 null，调度方跳过该车场
     */
    String buildConfigDataJson(String parkCode);
}
