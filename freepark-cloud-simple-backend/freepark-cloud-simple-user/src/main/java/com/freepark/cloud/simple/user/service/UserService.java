package com.freepark.cloud.simple.user.service;

import com.freepark.cloud.simple.common.auth.AuthContext;
import com.freepark.cloud.simple.common.auth.JwtUtil;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.user.dto.AdminCreateRequest;
import com.freepark.cloud.simple.user.dto.LoginResponse;
import com.freepark.cloud.simple.user.dto.UserItem;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,64}$");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PAGE_SIZE = 100;

    /** 连续登录失败达到该次数后锁定账号 */
    private static final int MAX_LOGIN_FAILURES = 5;
    /** 账号锁定持续时间 */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(String username, String password) {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(401, MessageKeys.AUTH_CREDENTIALS_INVALID));

        LocalDateTime now = SiteZoneTimes.nowUtc();
        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
            throw new BizException(401, MessageKeys.AUTH_ACCOUNT_LOCKED,
                    remainingLockMinutes(account.getLockedUntil(), now));
        }

        if (!passwordEncoder.matches(password, account.getPassword())) {
            registerLoginFailure(account, now);
            throw new BizException(401, MessageKeys.AUTH_CREDENTIALS_INVALID);
        }

        if (!Integer.valueOf(1).equals(account.getStatus())) {
            throw new BizException(401, MessageKeys.AUTH_ACCOUNT_DISABLED);
        }

        clearLoginFailures(account);

        return new LoginResponse(jwtUtil.generateToken(account.getUsername()),
                account.getUsername(), account.getNickname(), effectiveRole(account));
    }

    /** 累计连续失败次数，达到上限则锁定账号一段时间；失败次数按登录名维度记录在账号上。 */
    private void registerLoginFailure(UserAccount account, LocalDateTime now) {
        int attempts = (account.getFailedAttempts() == null ? 0 : account.getFailedAttempts()) + 1;
        if (attempts >= MAX_LOGIN_FAILURES) {
            // 锁定后清零计数，解锁后重新享有完整尝试次数
            account.setFailedAttempts(0);
            account.setLockedUntil(now.plus(LOCK_DURATION));
        } else {
            account.setFailedAttempts(attempts);
        }
        userRepository.save(account);
    }

    /** 登录成功或锁定已过期时清除失败痕迹。 */
    private void clearLoginFailures(UserAccount account) {
        boolean dirty = (account.getFailedAttempts() != null && account.getFailedAttempts() != 0)
                || account.getLockedUntil() != null;
        if (!dirty) {
            return;
        }
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        userRepository.save(account);
    }

    /** 剩余锁定分钟数，向上取整且至少 1 分钟。 */
    private long remainingLockMinutes(LocalDateTime lockedUntil, LocalDateTime now) {
        long seconds = Duration.between(now, lockedUntil).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    /**
     * 当前登录用户信息（任何已登录管理员可用）。
     */
    public UserItem me() {
        String username = AuthContext.getUsername();
        if (username == null) {
            throw new BizException(401, MessageKeys.AUTH_UNAUTHORIZED);
        }
        return userRepository.findByUsername(username)
                .map(this::toItem)
                .orElseThrow(() -> new BizException(401, MessageKeys.AUTH_UNAUTHORIZED));
    }

    /**
     * 分页查询管理员列表。
     */
    public PageResult<UserItem> pageAdmins(String keyword, int page, int size) {
        requireSuperAdmin();
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1,
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<UserAccount> result;
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            result = userRepository.findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(kw, kw, pageable);
        } else {
            result = userRepository.findAll(pageable);
        }
        List<UserItem> items = result.getContent().stream().map(this::toItem).toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    /**
     * 新增普通管理员。
     */
    public UserItem createAdmin(AdminCreateRequest request) {
        requireSuperAdmin();
        String username = request == null ? "" : trim(request.username());
        String password = request == null ? "" : (request.password() == null ? "" : request.password());
        String nickname = trim(request == null ? null : request.nickname());

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BizException(400, MessageKeys.USER_USERNAME_INVALID);
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BizException(400, MessageKeys.USER_PASSWORD_TOO_SHORT);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BizException(400, MessageKeys.USER_DUPLICATE);
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        account.setRole(UserAccount.ROLE_ADMIN);
        return toItem(userRepository.save(account));
    }

    /**
     * 启用 / 停用管理员。
     */
    public void updateStatus(Long id, Integer status) {
        UserAccount operator = requireSuperAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        UserAccount target = findByIdOrThrow(id);
        if (status == 0) {
            if (target.getId().equals(operator.getId())) {
                throw new BizException(400, MessageKeys.USER_SELF_STATUS);
            }
            if (UserAccount.ROLE_SUPER_ADMIN.equals(effectiveRole(target))) {
                throw new BizException(400, MessageKeys.USER_SUPER_ADMIN_PROTECTED);
            }
        }
        target.setStatus(status);
        userRepository.save(target);
    }

    /**
     * 重置管理员密码。
     */
    public void resetPassword(Long id, String password) {
        requireSuperAdmin();
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BizException(400, MessageKeys.USER_PASSWORD_TOO_SHORT);
        }
        UserAccount target = findByIdOrThrow(id);
        target.setPassword(passwordEncoder.encode(password));
        userRepository.save(target);
    }

    private UserAccount findByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException(404, MessageKeys.USER_NOT_FOUND));
    }

    /**
     * 当前登录用户必须是超级管理员，否则 403。
     */
    private UserAccount requireSuperAdmin() {
        String username = AuthContext.getUsername();
        if (username == null) {
            throw new BizException(401, MessageKeys.AUTH_UNAUTHORIZED);
        }
        UserAccount operator = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(403, MessageKeys.AUTH_FORBIDDEN));
        if (!Integer.valueOf(1).equals(operator.getStatus())
                || !UserAccount.ROLE_SUPER_ADMIN.equals(effectiveRole(operator))) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
        return operator;
    }

    /**
     * 历史数据未写 role 时兜底为普通管理员。
     */
    private String effectiveRole(UserAccount account) {
        String role = account.getRole();
        return StringUtils.hasText(role) ? role : UserAccount.ROLE_ADMIN;
    }

    private UserItem toItem(UserAccount account) {
        return new UserItem(account.getId(), account.getUsername(), account.getNickname(),
                effectiveRole(account), account.getStatus(), account.getCreatedAt());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
