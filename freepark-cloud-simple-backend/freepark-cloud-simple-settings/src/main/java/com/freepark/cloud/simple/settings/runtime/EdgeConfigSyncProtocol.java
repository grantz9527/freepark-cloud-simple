package com.freepark.cloud.simple.settings.runtime;

/**
 * 边缘配置同步协议 v3（edge.config.sync/3）的常量定义。
 *
 * <p>v3 相对 v2 的差异：不再把整包车场清单放进单条 retain 消息，而是为每个边缘节点
 * 生成一组<b>有序、有编号的全量帧</b>（同一主题“前缀/节点编号”，无 retain）。
 * 帧按“车场 → 业务域 → 条目分片”组织：单个数据量很大的域（如白名单、内部车、车位，
 * 单个车场可达数千上万条）会被切分为多条 ≤ {@link #MAX_ITEMS_PER_FRAME} 条目的帧。
 * 边缘侧先按 snapshotId 聚合完 total 帧，再做整批替换，天然规避大包与局部脏数据问题。</p>
 */
public final class EdgeConfigSyncProtocol {

    private EdgeConfigSyncProtocol() {
    }

    /** 当前协议 schema 标识（v3 全量帧信封） */
    public static final String SCHEMA = "edge.config.sync/3";
    public static final int SCHEMA_VERSION = 3;

    /** 全量快照帧（kind）：覆盖节点名下全部车场/域的条目，边缘聚合完成后整批替换 */
    public static final String KIND_FULL = "full";
    /**
     * 变更增量帧（kind）：携带单条或多条变更（entries 数组，每条形如
     * {@code {"op":"upsert"|"delete","item":{...}} 或 {"op":"delete","id":123}}），
     * 边缘聚合同一批次（snapshotId）的帧后按云端主键 id（lot 域按 code）精确应用。
     */
    public static final String KIND_DELTA = "delta";

    /** 增量条目操作：新增或修改（携带完整条目，边缘按条目自带 id/code 定位后整体覆盖） */
    public static final String DELTA_OP_UPSERT = "upsert";
    /** 增量条目操作：删除（仅携带云端主键 id，lot 域不使用删除） */
    public static final String DELTA_OP_DELETE = "delete";

    /** 业务域：车场自身配置 */
    public static final String DOMAIN_LOT = "lot";
    /** 业务域：黑名单 */
    public static final String DOMAIN_BLACKLIST = "blacklist";
    /** 业务域：车牌号段放行（正则名单） */
    public static final String DOMAIN_PATTERN = "pattern";
    /** 业务域：白名单（停车卡，含时间区间） */
    public static final String DOMAIN_WHITELIST = "whitelist";
    /** 业务域：内部车辆 */
    public static final String DOMAIN_INTERNAL = "internal";
    /** 业务域：车位管理（位置/区域/车位三层，单条帧以 type 区分） */
    public static final String DOMAIN_SPACE = "space";
    /** 业务域：通道（出入口，可关联对向车场） */
    public static final String DOMAIN_LANE = "lane";

    /** 单帧最多承载条目数：超过则按该上限拆成连续多帧（seq 递增、total 一致） */
    public static final int MAX_ITEMS_PER_FRAME = 1000;
}
