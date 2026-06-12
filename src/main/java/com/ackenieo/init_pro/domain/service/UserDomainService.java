package com.ackenieo.init_pro.domain.service;

import java.util.Optional;

import com.ackenieo.init_pro.domain.model.entity.User;

public interface UserDomainService {
    
    /**
     * 根据手机号查找用户
     */
    public Optional<User> findByPhone(String phone) ;

    /**
     * 创建新用户
     */
    public User createUser(String phone);
}
