package com.freepark.cloud.simple.settings.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.settings.dto.SystemSettingsView;
import com.freepark.cloud.simple.settings.dto.UpdateSystemSettingsRequest;
import com.freepark.cloud.simple.settings.entity.SystemSettings;
import com.freepark.cloud.simple.settings.repository.SystemSettingsRepository;
import com.freepark.cloud.simple.settings.support.SystemSettingsOptions;
import com.freepark.cloud.simple.settings.time.ConfiguredSiteZoneProvider;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 全局站点配置服务：单例读取/保存，仅超级管理员可修改。
 */
@Service
public class SystemSettingsService {

    private final SystemSettingsRepository settingsRepository;
    private final AdminGuard adminGuard;
    private final ConfiguredSiteZoneProvider siteZoneProvider;

    public SystemSettingsService(SystemSettingsRepository settingsRepository,
                                 AdminGuard adminGuard,
                                 ConfiguredSiteZoneProvider siteZoneProvider) {
        this.settingsRepository = settingsRepository;
        this.adminGuard = adminGuard;
        this.siteZoneProvider = siteZoneProvider;
    }

    /**
     * 读取站点配置（任意启用中的管理员可读，页面按菜单角色限制为超管）。
     */
    @Transactional(readOnly = true)
    public SystemSettingsView getSettings() {
        adminGuard.requireEnabledAdmin();
        return toView(requireSettings());
    }

    /**
     * 更新站点配置（仅超级管理员）。
     */
    @Transactional
    public SystemSettingsView updateSettings(UpdateSystemSettingsRequest request) {
        requireSuperAdmin();
        SystemSettings settings = requireSettings();

        String locale = SystemSettingsOptions.validateLocale(
                request == null ? null : request.defaultLocale());
        String timezone = SystemSettingsOptions.validateTimezone(
                request == null ? null : request.timezone());
        List<String> allowed = SystemSettingsOptions.normalizeAllowed(
                request == null ? null : request.allowedPlateColors());
        String defaultColor = SystemSettingsOptions.validatePlateColor(
                request == null ? null : request.defaultPlateColor());
        SystemSettingsOptions.ensureDefaultAllowed(defaultColor, allowed);
        List<String> allowedCurrencies = SystemSettingsOptions.normalizeAllowedCurrencies(
                request == null ? null : request.allowedCurrencies());
        String defaultCurrency = SystemSettingsOptions.validateCurrency(
                request == null ? null : request.defaultCurrency());
        SystemSettingsOptions.ensureDefaultCurrencyAllowed(defaultCurrency, allowedCurrencies);

        settings.setDefaultLocale(locale);
        settings.setTimezone(timezone);
        settings.setDefaultPlateColor(defaultColor);
        settings.setAllowedPlateColors(allowed);
        settings.setDefaultCurrency(defaultCurrency);
        settings.setAllowedCurrencies(allowedCurrencies);
        // saveAndFlush：让 @PreUpdate 在方法内执行并回写 updatedAt，
        // 使响应中的“最近更新”是本轮真实的 UTC 锚点（而非 flush 前的旧值）
        SystemSettingsView view = toView(settingsRepository.saveAndFlush(settings));
        // 时区可能已变化：立即刷新站点时区缓存，使后续所有接口的时间换算立刻生效
        siteZoneProvider.reload();
        return view;
    }

    /**
     * 返回默认单例配置；历史脏数据（空允许列表/空默认值）在此兜底规范化。
     */
    private SystemSettings requireSettings() {
        SystemSettings settings = settingsRepository.findById(SystemSettings.SINGLETON_ID)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        ensureDefaults(settings);
        return settings;
    }

    private void ensureDefaults(SystemSettings settings) {
        if (settings.getAllowedPlateColors() == null || settings.getAllowedPlateColors().isEmpty()) {
            settings.setAllowedPlateColors(SystemSettingsOptions.DEFAULT_ALLOWED_PLATE_COLORS);
        }
        if (settings.getDefaultPlateColor() == null
                || settings.getDefaultPlateColor().isBlank()) {
            settings.setDefaultPlateColor(SystemSettingsOptions.DEFAULT_PLATE_COLOR);
        }
        if (!settings.getAllowedPlateColors().contains(settings.getDefaultPlateColor())) {
            settings.setDefaultPlateColor(settings.getAllowedPlateColors().getFirst());
        }
        if (settings.getAllowedCurrencies() == null || settings.getAllowedCurrencies().isEmpty()) {
            settings.setAllowedCurrencies(SystemSettingsOptions.DEFAULT_ALLOWED_CURRENCIES);
        }
        if (settings.getDefaultCurrency() == null
                || settings.getDefaultCurrency().isBlank()) {
            settings.setDefaultCurrency(SystemSettingsOptions.DEFAULT_CURRENCY);
        }
        if (!settings.getAllowedCurrencies().contains(settings.getDefaultCurrency())) {
            settings.setDefaultCurrency(settings.getAllowedCurrencies().getFirst());
        }
    }

    private SystemSettingsView toView(SystemSettings settings) {
        ensureDefaults(settings);
        return new SystemSettingsView(
                settings.getDefaultLocale(),
                settings.getTimezone(),
                settings.getDefaultPlateColor(),
                List.copyOf(settings.getAllowedPlateColors()),
                settings.getDefaultCurrency(),
                List.copyOf(settings.getAllowedCurrencies()),
                SystemSettingsOptions.SUPPORTED_LOCALES,
                SystemSettingsOptions.SUPPORTED_TIMEZONES,
                SystemSettingsOptions.SUPPORTED_PLATE_COLORS,
                SystemSettingsOptions.SUPPORTED_CURRENCIES,
                settings.getUpdatedAt());
    }

    private void requireSuperAdmin() {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        String role = operator.getRole();
        if (!UserAccount.ROLE_SUPER_ADMIN.equals(role)) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
    }
}
