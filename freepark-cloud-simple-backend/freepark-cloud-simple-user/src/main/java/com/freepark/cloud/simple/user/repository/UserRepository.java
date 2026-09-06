package com.freepark.cloud.simple.user.repository;

import com.freepark.cloud.simple.user.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    /** 按用户名或昵称模糊搜索（忽略大小写） */
    Page<UserAccount> findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
            String username, String nickname, Pageable pageable);
}
