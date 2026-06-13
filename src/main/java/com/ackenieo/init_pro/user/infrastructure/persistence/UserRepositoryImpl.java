package com.ackenieo.init_pro.user.infrastructure.persistence;

import com.ackenieo.init_pro.user.domain.entity.User;
import com.ackenieo.init_pro.user.domain.repository.UserRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user;
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return Optional.ofNullable(userMapper.selectOne(wrapper));
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(userMapper.selectById(id));
    }

    @Override
    public boolean existsByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return userMapper.selectCount(wrapper) > 0;
    }
}
