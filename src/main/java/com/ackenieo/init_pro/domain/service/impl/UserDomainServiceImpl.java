package com.ackenieo.init_pro.domain.service.impl;

import com.ackenieo.init_pro.domain.model.entity.User;
import com.ackenieo.init_pro.domain.repository.UserRepository;
import com.ackenieo.init_pro.domain.service.UserDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 用户领域服务
 */
@Slf4j
@Service
public class UserDomainServiceImpl implements UserDomainService {
    // private static final Logger log = LoggerFactory.getLogger(UserDomainServiceImpl.class);

    private final UserRepository userRepository;
    //private final PasswordEncoder passwordEncoder;

    public UserDomainServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        //this.passwordEncoder = passwordEncoder;
    }

    /**
     * 根据手机号查找用户
     */
    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    /**
     * 创建新用户
     */
    @Override
    public User createUser(String phone) {
        User user = new User(phone);
        user.setNickname("用户" + phone.substring(7));
        log.info("创建新用户: {}", phone);
        return userRepository.save(user);
    }

    // /**
    //  * 设置密码
    //  */
    // public void setPassword(User user, String rawPassword) {
    //     user.encodeAndSetPassword(rawPassword, passwordEncoder);
    //     userRepository.save(user);
    //     log.info("用户 {} 设置密码", user.getPhone());
    // }

    // /**
    //  * 验证密码
    //  */
    // public boolean verifyPassword(User user, String rawPassword) {
    //     return user.matchesPassword(rawPassword, passwordEncoder);
    // }
}
