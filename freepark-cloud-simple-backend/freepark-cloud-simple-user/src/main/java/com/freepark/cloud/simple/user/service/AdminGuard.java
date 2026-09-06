package com.freepark.cloud.simple.user.service;

import com.freepark.cloud.simple.common.auth.AuthContext;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * 业务操作守卫：要求当前登录人必须是启用中的管理员（超管或普通管理员均可）。
 */
@Component
public class AdminGuard {

    private final UserRepository userRepository;

    public AdminGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 校验当前登录人身份并返回启用中的管理员账号。
     */
    public UserAccount requireEnabledAdmin() {
        String username = AuthContext.getUsername();
        if (username == null) {
            throw new BizException(401, MessageKeys.AUTH_UNAUTHORIZED);
        }
        UserAccount operator = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(403, MessageKeys.AUTH_FORBIDDEN));
        if (!Integer.valueOf(1).equals(operator.getStatus())) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
        return operator;
    }
}
