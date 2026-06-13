package com.ackenieo.init_pro.user.interfaces.dto;

/**
 * 发送验证码请求
 */
public class SendSmsRequest {
    private String phone;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
