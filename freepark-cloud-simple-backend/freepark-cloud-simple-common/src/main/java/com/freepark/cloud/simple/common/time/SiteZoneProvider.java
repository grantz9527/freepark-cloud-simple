package com.freepark.cloud.simple.common.time;

import java.time.ZoneId;

/**
 * 站点时区提供者：对外暴露「系统配置的站点时区」。
 * <p>
 * 约定：所有持久化时间一律以「UTC 挂钟时间」作为绝对时刻锚点存库；
 * 在接口边界（请求/响应）按本站点时区换算为本地挂钟时间展示/回传。
 * 因此底层模块不直接依赖 settings 模块，只依赖本接口，由 settings 模块提供实现。
 */
public interface SiteZoneProvider {

    /**
     * 当前系统配置的站点时区（读取失败或尚未配置时回退默认时区，永不为 null）。
     */
    ZoneId currentZone();
}
