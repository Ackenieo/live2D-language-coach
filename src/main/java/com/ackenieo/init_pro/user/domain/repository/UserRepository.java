package com.ackenieo.init_pro.user.domain.repository;

import com.ackenieo.init_pro.user.domain.entity.User;

import java.util.Optional;

/**
 * 用户仓储接口
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findByPhone(String phone);
    Optional<User> findById(String id);
}
