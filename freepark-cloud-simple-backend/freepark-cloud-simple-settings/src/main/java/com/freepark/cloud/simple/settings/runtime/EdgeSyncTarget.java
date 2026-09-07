package com.freepark.cloud.simple.settings.runtime;

/**
 * 一个参与配置下发的车场目标。实现方只负责给出“车场编码”，
 * 主题拼接（{配置同步发布主题前缀}/{车场编码}）、长度上限与负载构建由调度方统一处理。
 *
 * @param parkCode 车场编码，需可安全作为 MQTT 主题段使用
 */
public record EdgeSyncTarget(String parkCode) {
}
