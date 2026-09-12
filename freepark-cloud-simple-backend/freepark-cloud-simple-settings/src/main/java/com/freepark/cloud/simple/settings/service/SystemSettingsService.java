package com.freepark.cloud.simple.settings.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.settings.dto.PublicSiteSettingsView;
import com.freepark.cloud.simple.settings.dto.SystemSettingsView;
import com.freepark.cloud.simple.settings.dto.UpdateSystemSettingsRequest;
import com.freepark.cloud.simple.settings.entity.SystemSettings;
import com.freepark.cloud.simple.settings.pay.ConfiguredSiteBaseUrlProvider;
import com.freepark.cloud.simple.settings.pay.ConfiguredUserBaseUrlProvider;
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
    private final ConfiguredSiteBaseUrlProvider siteBaseUrlProvider;
    private final ConfiguredUserBaseUrlProvider userBaseUrlProvider;
    private final WeChatConfigService weChatConfigService;

    public SystemSettingsService(SystemSettingsRepository settingsRepository,
                                 AdminGuard adminGuard,
                                 ConfiguredSiteZoneProvider siteZoneProvider,
                                 ConfiguredSiteBaseUrlProvider siteBaseUrlProvider,
                                 ConfiguredUserBaseUrlProvider userBaseUrlProvider,
                                 WeChatConfigService weChatConfigService) {
        this.settingsRepository = settingsRepository;
        this.adminGuard = adminGuard;
        this.siteZoneProvider = siteZoneProvider;
        this.siteBaseUrlProvider = siteBaseUrlProvider;
        this.userBaseUrlProvider = userBaseUrlProvider;
        this.weChatConfigService = weChatConfigService;
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
     * C 端（用户端网页）公开读取最小站点配置：车牌版式、语言、币种与开放的缴费方式。
     * 供免登录的查费/缴费页渲染。不需要管理员身份。
     */
    @Transactional(readOnly = true)
    public PublicSiteSettingsView getPublicSettings() {
        return toPublicView(requireSettings());
    }

    /**
     * 当前站点开放的线上缴费方式（与系统配置一致）。公开缴款下单时据此校验，无需管理员身份。
     */
    @Transactional(readOnly = true)
    public List<String> getAllowedPaymentMethods() {
        return List.copyOf(requireSettings().getAllowedPaymentMethods());
    }

    /** 用户端是否必须一次缴清该车牌全部欠费。 */
    @Transactional(readOnly = true)
    public boolean isForcePayAll() {
        return requireSettings().isForcePayAll();
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
        String plateRegion = SystemSettingsOptions.validatePlateRegion(
                request == null ? null : request.plateRegion());
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
        List<String> allowedPaymentMethods = SystemSettingsOptions.normalizeAllowedPaymentMethods(
                request == null ? null : request.allowedPaymentMethods());
        String adminBaseUrl = SystemSettingsOptions.validateSiteBaseUrl(
                request == null ? null : request.adminBaseUrl());
        String userBaseUrl = SystemSettingsOptions.validateSiteBaseUrl(
                request == null ? null : request.userBaseUrl());
        boolean forcePayAll = request == null || request.forcePayAll() == null || request.forcePayAll();

        settings.setDefaultLocale(locale);
        settings.setTimezone(timezone);
        settings.setPlateRegion(plateRegion);
        settings.setDefaultPlateColor(defaultColor);
        settings.setAllowedPlateColors(allowed);
        settings.setDefaultCurrency(defaultCurrency);
        settings.setAllowedCurrencies(allowedCurrencies);
        settings.setAllowedPaymentMethods(allowedPaymentMethods);
        settings.setAdminBaseUrl(adminBaseUrl);
        settings.setUserBaseUrl(userBaseUrl);
        settings.setForcePayAll(forcePayAll);
        // saveAndFlush：让 @PreUpdate 在方法内执行并回写 updatedAt，
        // 使响应中的“最近更新”是本轮真实的 UTC 锚点（而非 flush 前的旧值）
        SystemSettingsView view = toView(settingsRepository.saveAndFlush(settings));
        // 时区可能已变化：立即刷新站点时区缓存，使后续所有接口的时间换算立刻生效
        siteZoneProvider.reload();
        siteBaseUrlProvider.reload();
        userBaseUrlProvider.reload();
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
        if (settings.getPlateRegion() == null || settings.getPlateRegion().isBlank()) {
            // 历史库在 plate_region 列引入前创建：兜底为默认 CN
            settings.setPlateRegion(SystemSettingsOptions.DEFAULT_PLATE_REGION);
        }
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
        if (settings.getAllowedPaymentMethods() == null
                || settings.getAllowedPaymentMethods().isEmpty()) {
            settings.setAllowedPaymentMethods(SystemSettingsOptions.DEFAULT_ALLOWED_PAYMENT_METHODS);
        }
        if (settings.getAdminBaseUrl() == null) {
            settings.setAdminBaseUrl("");
        }
        if (settings.getUserBaseUrl() == null) {
            settings.setUserBaseUrl("");
        }
        settings.setForcePayAll(settings.isForcePayAll());
    }

    private SystemSettingsView toView(SystemSettings settings) {
        ensureDefaults(settings);
        return new SystemSettingsView(
                settings.getDefaultLocale(),
                settings.getTimezone(),
                settings.getAdminBaseUrl(),
                settings.getUserBaseUrl(),
                settings.getPlateRegion(),
                settings.getDefaultPlateColor(),
                List.copyOf(settings.getAllowedPlateColors()),
                settings.getDefaultCurrency(),
                List.copyOf(settings.getAllowedCurrencies()),
                List.copyOf(settings.getAllowedPaymentMethods()),
                settings.isForcePayAll(),
                SystemSettingsOptions.SUPPORTED_LOCALES,
                SystemSettingsOptions.SUPPORTED_TIMEZONES,
                SystemSettingsOptions.SUPPORTED_PLATE_REGIONS,
                SystemSettingsOptions.SUPPORTED_PLATE_COLORS,
                SystemSettingsOptions.SUPPORTED_CURRENCIES,
                SystemSettingsOptions.SUPPORTED_PAYMENT_METHODS,
                settings.getUpdatedAt());
    }

    private PublicSiteSettingsView toPublicView(SystemSettings settings) {
        ensureDefaults(settings);
        String userBase = settings.getUserBaseUrl();
        if (userBase == null || userBase.isBlank()) {
            // 公开接口返回有效跳转根：未配用户端时回落后台地址
            userBase = settings.getAdminBaseUrl();
        }
        return new PublicSiteSettingsView(
                settings.getPlateRegion(),
                settings.getDefaultLocale(),
                settings.getDefaultCurrency(),
                List.copyOf(settings.getAllowedPaymentMethods()),
                userBase == null ? "" : userBase,
                weChatConfigService.publicMpAppId(),
                settings.isForcePayAll());
    }

    private void requireSuperAdmin() {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        String role = operator.getRole();
        if (!UserAccount.ROLE_SUPER_ADMIN.equals(role)) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
    }
}
