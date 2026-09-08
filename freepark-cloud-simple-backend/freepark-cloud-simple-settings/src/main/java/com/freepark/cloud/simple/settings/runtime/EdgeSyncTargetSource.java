package com.freepark.cloud.simple.settings.runtime;

import java.util.List;

/**
 * 边缘配置同步的目标来源：枚举当前应参与“云端→边缘节点”下发的节点同步单元。
 *
 * <p>接口定义在 settings（不感知具体业务实体）；由提供车场数据的业务模块
 * （如 parking）实现并注册为 Spring Bean。</p>
 */
public interface EdgeSyncTargetSource {

    /**
     * 返回本周期应下发的边缘节点目标列表。
     * 实现方需自行过滤：仅返回启用、且编号可作为 MQTT 主题段（无空白/通配符/斜杠）
     * 的边缘节点；每个目标携带该节点应管辖的车场编码集合（可为空）。
     */
    List<EdgeSyncTarget> targets();
}
