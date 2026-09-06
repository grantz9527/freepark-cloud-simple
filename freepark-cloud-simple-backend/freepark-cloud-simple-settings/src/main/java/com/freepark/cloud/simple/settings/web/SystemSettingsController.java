package com.freepark.cloud.simple.settings.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.settings.dto.SystemSettingsView;
import com.freepark.cloud.simple.settings.dto.UpdateSystemSettingsRequest;
import com.freepark.cloud.simple.settings.service.SystemSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置：区域与语言、车牌颜色等全局站点配置（写操作仅超级管理员）。
 */
@RestController
@RequestMapping("/api/system/settings")
public class SystemSettingsController {

    private final SystemSettingsService settingsService;

    public SystemSettingsController(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ApiResult<SystemSettingsView> get() {
        return ApiResult.ok(settingsService.getSettings());
    }

    @PutMapping
    public ApiResult<SystemSettingsView> update(@RequestBody UpdateSystemSettingsRequest request) {
        return ApiResult.ok(settingsService.updateSettings(request));
    }
}
