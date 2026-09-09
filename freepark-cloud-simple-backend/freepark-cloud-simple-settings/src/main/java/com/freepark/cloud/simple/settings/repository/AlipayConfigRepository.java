package com.freepark.cloud.simple.settings.repository;

import com.freepark.cloud.simple.settings.entity.AlipayConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 支付宝支付配置仓储（单例，id = "default"）。
 */
public interface AlipayConfigRepository extends JpaRepository<AlipayConfig, String> {
}
