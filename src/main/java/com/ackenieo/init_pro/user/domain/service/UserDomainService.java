package com.ackenieo.init_pro.user.domain.service;

import com.ackenieo.init_pro.user.domain.entity.User;
import com.ackenieo.init_pro.user.domain.repository.UserRepository;

import java.util.Optional;

public interface UserDomainService {
    Optional<User> findByPhone(String phone);
    User createUser(String phone);
}
