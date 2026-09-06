package com.freepark.cloud.simple.settings.repository;

import com.freepark.cloud.simple.settings.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 站点配置仓储（单例，id = "default"）。
 */
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, String> {
}
