package com.freepark.cloud.simple.settings.time;

import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.settings.entity.SystemSettings;
import com.freepark.cloud.simple.settings.repository.SystemSettingsRepository;
import com.freepark.cloud.simple.settings.support.SystemSettingsOptions;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

/**
 * 基于系统配置实现的站点时区提供者。
 * <p>
 * 负责在应用内以低成本读取「系统配置时区」：时区值变化频率极低，这里做缓存；
 * 系统配置更新（updateSettings）后调用 {@link #reload()} 失效缓存即可。
 * Jackson 全局换算（LocalDateTime ↔ 站点本地时间）也统一走本提供者。
 */
@Service
public class ConfiguredSiteZoneProvider implements SiteZoneProvider {

    private final SystemSettingsRepository settingsRepository;

    /** 缓存的站点时区；首次访问时懒加载。 */
    private volatile ZoneId cachedZone;

    public ConfiguredSiteZoneProvider(SystemSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public ZoneId currentZone() {
        ZoneId zone = cachedZone;
        if (zone == null) {
            synchronized (this) {
                zone = cachedZone;
                if (zone == null) {
                    zone = loadZone();
                    cachedZone = zone;
                }
            }
        }
        return zone;
    }

    /**
     * 配置被更新后调用：重新读取系统配置时区并刷新缓存。
     */
    public synchronized void reload() {
        cachedZone = loadZone();
    }

    /**
     * 读取配置（DB 尚未就绪/读取失败时回退默认时区，保证任何时刻都能给出合法时区）。
     */
    private ZoneId loadZone() {
        try {
            SystemSettings settings = settingsRepository
                    .findById(SystemSettings.SINGLETON_ID)
                    .orElse(null);
            return SystemSettingsOptions.zoneIdOrDefault(settings == null ? null : settings.getTimezone());
        } catch (RuntimeException e) {
            return SystemSettingsOptions.zoneIdOrDefault(null);
        }
    }
}
