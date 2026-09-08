package com.freepark.cloud.simple.parking.edge;

import java.util.List;

/**
 * 某车场某业务域的“单次写事务内变更集合”事件：由业务 Service 在写事务中通过
 * {@link EdgeDomainChangeNotifier} 发布，事务提交后由 {@link EdgeDomainDeltaPublisher}
 * 消费并转换为 v3 kind=delta 增量帧下发到管理该车场的边缘节点。
 *
 * <p>语义：op 取值见 {@link com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol}
 * 的 DELTA_OP_UPSERT / DELTA_OP_DELETE。upsert 的 item 为已持久化的业务实体
 * （保存后主键已回填），delete 的 item 为被删记录的云端主键 Long。
 * 事件仅在业务写入成功后发布一次；回滚的事务不会产生任何增量。</p>
 */
public record EdgeConfigDomainChangedEvent(String domain, String parkCode, List<Change> changes) {

    /** 单条变更：操作 + 目标（upsert=实体实例；delete=云端主键 Long） */
    public record Change(String op, Object item) {
    }

    public boolean isEmpty() {
        return changes == null || changes.isEmpty();
    }
}
