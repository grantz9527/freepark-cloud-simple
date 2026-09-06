package com.freepark.cloud.simple.user.config;

import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化默认管理员账号：账号不存在时创建；存在时将其密码重置为配置值。
 * 仅在 freepark.user.init.enabled=true 时生效（默认开启，生产环境请关闭或修改默认密码）。
 */
@Component
public class DefaultUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean initEnabled;
    private final String username;
    private final String password;

    public DefaultUserInitializer(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${freepark.user.init.enabled:true}") boolean initEnabled,
                                  @Value("${freepark.user.init.username:admin}") String username,
                                  @Value("${freepark.user.init.password:admin123}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initEnabled = initEnabled;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!initEnabled) {
            return;
        }
        userRepository.findByUsername(username).ifPresentOrElse(
                account -> {
                    account.setPassword(passwordEncoder.encode(password));
                    account.setRole(UserAccount.ROLE_SUPER_ADMIN);
                    userRepository.save(account);
                    log.warn("已将默认管理员账号 [{}] 密码重置为默认密码", username);
                },
                () -> {
                    UserAccount account = new UserAccount();
                    account.setUsername(username);
                    account.setPassword(passwordEncoder.encode(password));
                    account.setNickname("系统管理员");
                    account.setRole(UserAccount.ROLE_SUPER_ADMIN);
                    userRepository.save(account);
                    log.warn("已创建默认管理员账号：[{}]", username);
                }
        );
    }
}
