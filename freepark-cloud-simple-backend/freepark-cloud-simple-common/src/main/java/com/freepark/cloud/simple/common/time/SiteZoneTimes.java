package com.freepark.cloud.simple.common.time;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 站点时区换算工具（核心时间语义）。
 * <p>
 * 全局约定：<b>数据库内所有时间字段统一存「UTC 挂钟时间」</b>，即把某个绝对时刻换算成
 * UTC 时区的本地钟面读数后存储，作为锚点；在接口边界再按「系统配置的站点时区」换算成
 * 站点的本地钟面读数。这样切换系统配置时区后，同一记录在界面上的展示时间会随之整体偏移。
 * <p>
 * 换算一律使用目标时区规则计算偏移（兼容夏令时/历史偏移），不做固定 +8 之类的常量假设。
 */
public final class SiteZoneTimes {

    /** 站点时区缺省值（与 settings 模块 SystemSettingsOptions.DEFAULT_TIMEZONE 保持一致）。 */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private SiteZoneTimes() {
    }

    /** 当前绝对时刻的 UTC 锚点（新增/更新时间统一使用此方法写入）。 */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * 站点本地挂钟时间 → 库内 UTC 锚点。
     * <p>
     * 请求/界面上用户看到的「站点本地时间」先换算成 UTC 锚点再入库。
     * 夏令时缺口等本地不存在的时刻，按该时区规则给出的偏移换算，避免直接抛错。
     *
     * @param siteWall 站点本地钟面读数
     * @param zone     站点时区
     */
    public static LocalDateTime toUtcAnchor(LocalDateTime siteWall, ZoneId zone) {
        if (siteWall == null || zone == null) {
            return siteWall;
        }
        return siteWall.toInstant(zone.getRules().getOffset(siteWall))
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /**
     * 库内 UTC 锚点 → 站点本地挂钟时间。
     * <p>
     * 列表/编辑回显、导出等需要把库内 UTC 锚点换算成「系统配置时区」的本地时间。
     */
    public static LocalDateTime toSiteWall(LocalDateTime utcAnchor, ZoneId zone) {
        if (utcAnchor == null || zone == null) {
            return utcAnchor;
        }
        return utcAnchor.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(zone)
                .toLocalDateTime();
    }
}
