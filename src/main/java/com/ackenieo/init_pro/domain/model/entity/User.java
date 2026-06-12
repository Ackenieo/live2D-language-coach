package com.ackenieo.init_pro.domain.model.entity;

import com.ackenieo.init_pro.domain.model.entity.base.BaseEntity;
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

    // /**
    //  * 设置密码（加密后）
    //  */
    // public void encodeAndSetPassword(String rawPassword, org.springframework.security.crypto.password.PasswordEncoder encoder) {
    //     this.password = encoder.encode(rawPassword);
    // }

    // /**
    //  * 验证密码
    //  */
    // public boolean matchesPassword(String rawPassword, org.springframework.security.crypto.password.PasswordEncoder encoder) {
    //     if (this.password == null) {
    //         return false;
    //     }
    //     return encoder.matches(rawPassword, this.password);
    // }
}
