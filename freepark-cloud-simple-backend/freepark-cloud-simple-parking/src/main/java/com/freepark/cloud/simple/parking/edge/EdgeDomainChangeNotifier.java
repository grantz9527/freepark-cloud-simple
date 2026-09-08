package com.freepark.cloud.simple.parking.edge;

import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务写事务内的“边缘配置域变更”发布口：各可同步数据域（lot/lane/blacklist/pattern/
 * whitelist/internal/space）的 Service 在持久化成功后调用对应方法发布事件，
 * 由 AFTER_COMMIT 监听器在事务提交后转成增量帧。单条写用单条目方法，
 * 批量导入/批量删除用批量方法（一次事件携带全部变更，由增量发布器分帧）。
 */
@Component
public class EdgeDomainChangeNotifier {

    private final ApplicationEventPublisher events;

    public EdgeDomainChangeNotifier(ApplicationEventPublisher events) {
        this.events = events;
    }

    /** 单条新增或修改：item 为已保存（主键已回填）的实体 */
    public void upsert(String domain, String parkCode, Object item) {
        if (parkCode == null || parkCode.isBlank() || item == null) {
            return;
        }
        publish(domain, parkCode, EdgeConfigSyncProtocol.DELTA_OP_UPSERT, List.of(item));
    }

    /** 批量新增或修改（导入等）：items 为已保存实体集合 */
    public void upserts(String domain, String parkCode, List<?> items) {
        if (parkCode == null || parkCode.isBlank() || items == null || items.isEmpty()) {
            return;
        }
        publish(domain, parkCode, EdgeConfigSyncProtocol.DELTA_OP_UPSERT, items);
    }

    /** 单条删除：id 为被删记录的云端主键 */
    public void delete(String domain, String parkCode, Long id) {
        if (parkCode == null || parkCode.isBlank() || id == null) {
            return;
        }
        publish(domain, parkCode, EdgeConfigSyncProtocol.DELTA_OP_DELETE, List.of(id));
    }

    /** 批量删除：ids 为被删记录的云端主键集合 */
    public void deletes(String domain, String parkCode, List<Long> ids) {
        if (parkCode == null || parkCode.isBlank() || ids == null || ids.isEmpty()) {
            return;
        }
        publish(domain, parkCode, EdgeConfigSyncProtocol.DELTA_OP_DELETE, ids);
    }

    private void publish(String domain, String parkCode, String op, List<?> items) {
        List<EdgeConfigDomainChangedEvent.Change> changes = items.stream()
                .map(item -> new EdgeConfigDomainChangedEvent.Change(op, item))
                .toList();
        events.publishEvent(new EdgeConfigDomainChangedEvent(domain, parkCode, changes));
    }
}
