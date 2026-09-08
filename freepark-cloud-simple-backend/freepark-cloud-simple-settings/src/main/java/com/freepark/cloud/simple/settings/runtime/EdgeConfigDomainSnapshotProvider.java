package com.freepark.cloud.simple.settings.runtime;

/**
 * 业务域数据快照构建器（v3 全量快照的“域条目”SPI）：针对某个车场构建某个业务域
 * 当前全部条目的 JSON 数组文本。
 *
 * <p>接口定义在 settings（不感知具体业务实体）；由提供车场数据的业务模块（如 parking）
 * 为各类可同步业务数据（黑名单、正则名单、白名单、内部车、车位、通道等）分别提供实现，并注册为
 * Spring Bean。调度方在给某个边缘节点的某个车场组织 v3 全量帧时，逐个调用域构建器，
 * 把返回的条目数组按 {@link EdgeConfigSyncProtocol#MAX_ITEMS_PER_FRAME} 上限切分成连续帧。</p>
 *
 * <p>约定：同一车场同一域的返回数组内容即“云端当前全量”，空数组（{@code []}）表示
 * 该域当前无条目；边缘侧整批替换后应恰好等于该数组。</p>
 */
public interface EdgeConfigDomainSnapshotProvider {

    /**
     * 业务域标识，取 {@link EdgeConfigSyncProtocol#DOMAIN_LOT} 之外的值
     * （lot 域由 EdgeConfigSnapshotBuilder 单独提供）。
     *
     * @return 非空、且不与其他域构建器重复的域标识
     */
    String domain();

    /**
     * 构建车场该域当前全部条目的 JSON 数组文本。
     *
     * @param parkCode 车场编码（来自边缘节点名下 EdgeSyncTarget.parkCodes()）
     * @return 合法 JSON 数组文本；车场已删除时返回空数组；构建失败抛运行时异常由调度方记录跳过
     */
    String buildDomainItemsJson(String parkCode);
}
