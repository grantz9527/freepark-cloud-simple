package com.freepark.cloud.simple.user.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.user.dto.AdminCreateRequest;
import com.freepark.cloud.simple.user.dto.AdminPasswordRequest;
import com.freepark.cloud.simple.user.dto.AdminStatusRequest;
import com.freepark.cloud.simple.user.dto.UserItem;
import com.freepark.cloud.simple.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员管理（仅超级管理员可访问，角色校验在 service 层完成）。
 */
@RestController
@RequestMapping("/api/user/admin")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页搜索管理员列表。
     */
    @GetMapping("/list")
    public ApiResult<PageResult<UserItem>> list(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(userService.pageAdmins(keyword, page, size));
    }

    /**
     * 新增普通管理员。
     */
    @PostMapping
    public ApiResult<UserItem> create(@RequestBody AdminCreateRequest request) {
        return ApiResult.ok(userService.createAdmin(request));
    }

    /**
     * 启用 / 停用管理员。
     */
    @PutMapping("/{id}/status")
    public ApiResult<Void> updateStatus(@PathVariable Long id,
                                        @RequestBody AdminStatusRequest request) {
        userService.updateStatus(id, request.status());
        return ApiResult.ok();
    }

    /**
     * 重置管理员密码。
     */
    @PutMapping("/{id}/password")
    public ApiResult<Void> resetPassword(@PathVariable Long id,
                                         @RequestBody AdminPasswordRequest request) {
        userService.resetPassword(id, request.password());
        return ApiResult.ok();
    }
}
