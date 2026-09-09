package com.freepark.cloud.simple.settings.repository;

import com.freepark.cloud.simple.settings.entity.WeChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 微信支付配置仓储（单例，id = "default"）。
 */
public interface WeChatConfigRepository extends JpaRepository<WeChatConfig, String> {
}
