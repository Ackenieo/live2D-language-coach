package com.ackenieo.init_pro.user.domain.service;

import com.ackenieo.init_pro.user.domain.entity.User;
import com.ackenieo.init_pro.user.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户领域服务
 */
@Service
public class UserDomainServiceImpl implements UserDomainService {
    private static final Logger log = LoggerFactory.getLogger(UserDomainServiceImpl.class);

    private final UserRepository userRepository;

    public UserDomainServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Override
    public User createUser(String phone) {
        User user = new User(phone);
        user.setNickname("\u7528\u6237" + phone.substring(7));
        log.info("\u521b\u5efa\u65b0\u7528\u6237: {}", phone);
        return userRepository.save(user);
    }
}
