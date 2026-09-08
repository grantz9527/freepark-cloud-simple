package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;

/**
 * 边缘配置全量快照条目序列化的共用小工具：可空字段在负载中直接省略
 * （“缺省即空”约定，边缘解析时视为 null，避免把大量空字段写进包）。
 */
final class EdgeSnapshotJson {

    private EdgeSnapshotJson() {
    }

    /** 仅当值非空时写入字段 */
    static void putIfPresent(ObjectNode node, String field, String value) {
        if (value != null) {
            node.put(field, value);
        }
    }

    /** 仅当时间为空时省略；否则以 ISO 文本（UTC 本地时刻，无时区后缀）下发 */
    static void putTimeIfPresent(ObjectNode node, String field, LocalDateTime value) {
        if (value != null) {
            node.put(field, value.toString());
        }
    }
}
