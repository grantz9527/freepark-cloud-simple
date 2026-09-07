package com.freepark.cloud.simple.settings.runtime;

/**
 * 边缘计算 MQTT 配置保存成功后发布的内部事件。
 * 运行时组件（连接管理、配置下发等）监听该事件以立即对齐最新配置；
 * 仅在事务提交后触发，避免把长连接变更塞进保存事务内。
 */
public record EdgeMqttConfigChangedEvent() {
}
