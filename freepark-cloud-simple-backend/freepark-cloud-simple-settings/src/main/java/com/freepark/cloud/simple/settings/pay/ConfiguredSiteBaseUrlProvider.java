package com.freepark.cloud.simple.settings.pay;

import com.freepark.cloud.simple.common.pay.SiteBaseUrlProvider;
import com.freepark.cloud.simple.settings.entity.SystemSettings;
import com.freepark.cloud.simple.settings.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于系统配置「后台基础地址」的公网根地址提供者。
 * 配置更新后调用 {@link #reload()} 刷新缓存，使支付回调 URL 立刻按新地址拼接。
 */
@Service
public class ConfiguredSiteBaseUrlProvider implements SiteBaseUrlProvider {

    private static final String UNSET = "";

    private final SystemSettingsRepository settingsRepository;

    private volatile String cachedBaseUrl;

    public ConfiguredSiteBaseUrlProvider(SystemSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public String currentBaseUrl() {
        String value = cachedBaseUrl;
        if (value == null) {
            synchronized (this) {
                value = cachedBaseUrl;
                if (value == null) {
                    value = loadBaseUrl();
                    cachedBaseUrl = value;
                }
            }
        }
        return value;
    }

    public synchronized void reload() {
        cachedBaseUrl = loadBaseUrl();
    }

    private String loadBaseUrl() {
        try {
            SystemSettings settings = settingsRepository
                    .findById(SystemSettings.SINGLETON_ID)
                    .orElse(null);
            String url = settings == null ? null : settings.getAdminBaseUrl();
            return StringUtils.hasText(url) ? url.trim() : UNSET;
        } catch (RuntimeException e) {
            return UNSET;
        }
    }
}
