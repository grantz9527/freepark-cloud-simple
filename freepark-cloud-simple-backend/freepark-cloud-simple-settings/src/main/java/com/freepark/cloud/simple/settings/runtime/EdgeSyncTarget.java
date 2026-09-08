package com.freepark.cloud.simple.settings.runtime;

import java.util.List;

/**
 * 一个参与配置下发的“边缘节点同步单元”：以边缘节点自身编号作为 MQTT 寻址目标，
 * 并携带该节点应管辖的车场编码集合。
 *
 * <p>主题拼接（{@code {配置同步发布主题前缀}/{节点编号}}）、长度上限与负载构建由调度方
 * 统一处理。一个节点可以只管辖一个车场（1:1），也可以同时管辖多个车场（1:N）；
 * 车场随节点绑定关系在每次同步周期自动下发/摘除，边缘侧无需预知车场清单。</p>
 *
 * @param nodeCode  边缘节点编号，需可安全作为 MQTT 主题段使用
 * @param parkCodes 该节点应管辖的车场编码集合（可为空，表示节点暂未绑定车场）
 */
public record EdgeSyncTarget(String nodeCode, List<String> parkCodes) {

    public EdgeSyncTarget {
        parkCodes = parkCodes == null ? List.of() : List.copyOf(parkCodes);
    }
}
