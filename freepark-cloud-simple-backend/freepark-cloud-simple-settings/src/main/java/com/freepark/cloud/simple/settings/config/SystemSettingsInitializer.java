package com.freepark.cloud.simple.settings.config;

import com.freepark.cloud.simple.settings.entity.SystemSettings;
import com.freepark.cloud.simple.settings.repository.SystemSettingsRepository;
import com.freepark.cloud.simple.settings.support.SystemSettingsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化默认站点配置（单例记录不存在时创建）。
 */
@Component
public class SystemSettingsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingsInitializer.class);

    private final SystemSettingsRepository settingsRepository;

    public SystemSettingsInitializer(SystemSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (settingsRepository.existsById(SystemSettings.SINGLETON_ID)) {
            return;
        }
        SystemSettings settings = new SystemSettings(
                SystemSettingsOptions.DEFAULT_LOCALE,
                SystemSettingsOptions.DEFAULT_TIMEZONE,
                SystemSettingsOptions.DEFAULT_PLATE_COLOR,
                SystemSettingsOptions.DEFAULT_ALLOWED_PLATE_COLORS);
        settingsRepository.save(settings);
        log.info("已初始化默认站点配置：locale={}, timezone={}, defaultPlateColor={}",
                settings.getDefaultLocale(), settings.getTimezone(), settings.getDefaultPlateColor());
    }
}
