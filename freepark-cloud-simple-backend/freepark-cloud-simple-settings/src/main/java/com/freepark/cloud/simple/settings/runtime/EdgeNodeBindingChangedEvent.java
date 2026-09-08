package com.freepark.cloud.simple.settings.runtime;

import java.util.List;

/**
 * 边缘节点“同步目标关系”变更事件（车场↔节点绑定/解绑、节点停用/启用/删除），
 * 由车场业务模块在写事务内发布；settings 配置同步调度器在事务提交后消费：
 * 仍属同步目标的节点立即补全量，已被摘除的节点则向其主题下发空快照用于清理边缘本地配置。
 */
public record EdgeNodeBindingChangedEvent(List<String> nodeCodes) {

    /** 需处理的节点均非空时才构建；返回 null 表示无需任何处理 */
    public static EdgeNodeBindingChangedEvent of(List<String> codes) {
        List<String> distinct = codes == null ? List.of() : codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        return distinct.isEmpty() ? null : new EdgeNodeBindingChangedEvent(distinct);
    }
}
