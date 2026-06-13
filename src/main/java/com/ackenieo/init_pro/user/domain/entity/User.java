package com.ackenieo.init_pro.user.domain.entity;

import com.ackenieo.init_pro.shared.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户实体
 */
@TableName("t_user")
public class User extends BaseEntity {
    private String phone;
    private String password;
    private String nickname;

    public User() {
    }

    public User(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
