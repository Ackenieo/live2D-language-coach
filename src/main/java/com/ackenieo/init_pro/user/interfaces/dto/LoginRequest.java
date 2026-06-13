package com.ackenieo.init_pro.user.interfaces.dto;

/**
 * 登录请求
 */
public class LoginRequest {
    private String phone;
    private String code;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
