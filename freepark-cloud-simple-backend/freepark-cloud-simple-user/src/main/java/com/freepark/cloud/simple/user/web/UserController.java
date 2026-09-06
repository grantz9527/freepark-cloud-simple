package com.freepark.cloud.simple.user.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.i18n.MessageService;
import com.freepark.cloud.simple.user.dto.LoginRequest;
import com.freepark.cloud.simple.user.dto.LoginResponse;
import com.freepark.cloud.simple.user.dto.UserItem;
import com.freepark.cloud.simple.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final MessageService messageService;

    public UserController(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    /**
     * 登录。
     */
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();
        if (username.isEmpty() || password.isEmpty()) {
            return ApiResult.fail(400, messageService.get(MessageKeys.AUTH_CREDENTIALS_EMPTY));
        }
        try {
            ApiResult<LoginResponse> result = ApiResult.ok(userService.login(username, password));
            result.setMessage(messageService.get(MessageKeys.COMMON_OK));
            return result;
        } catch (BizException e) {
            return ApiResult.fail(e.getCode(), messageService.get(e.getMessageKey()));
        }
    }

    /**
     * 当前登录管理员信息。
     */
    @GetMapping("/me")
    public ApiResult<UserItem> me() {
        return ApiResult.ok(userService.me());
    }
}
