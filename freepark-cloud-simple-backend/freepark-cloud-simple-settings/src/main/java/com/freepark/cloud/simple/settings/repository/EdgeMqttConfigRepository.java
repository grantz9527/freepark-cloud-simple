package com.freepark.cloud.simple.settings.repository;

import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 边缘计算（MQTT）配置仓储（单例，id = "default"）。
 */
public interface EdgeMqttConfigRepository extends JpaRepository<EdgeMqttConfig, String> {
}
