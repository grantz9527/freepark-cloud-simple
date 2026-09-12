package com.freepark.cloud.simple.settings.web;

import com.freepark.cloud.simple.settings.dto.PublicSiteSettingsView;
import com.freepark.cloud.simple.settings.service.SystemSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端（用户端网页）公开站点配置接口：免登录。
 * 该命名空间在 WebAuthConfig 中整体放行（免 JWT），仅暴露默认车牌版式/语言等页面渲染所需的最小信息。
 */
@RestController
@RequestMapping("/api/public")
public class PublicSiteSettingsController {

    private final SystemSettingsService settingsService;

    public PublicSiteSettingsController(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * 查询用户端默认 UI 基准（车牌版式、语言、币种与开放的缴费方式）。
     */
    @GetMapping("/site-settings")
    public PublicSiteSettingsView get() {
        return settingsService.getPublicSettings();
    }
}
