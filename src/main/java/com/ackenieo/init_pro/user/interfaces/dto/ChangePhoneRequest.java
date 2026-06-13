package com.ackenieo.init_pro.user.interfaces.dto;

/**
 * 换绑手机号请求
 */
public class ChangePhoneRequest {
    private String newPhone;
    private String code;

    public String getNewPhone() {
        return newPhone;
    }

    public void setNewPhone(String newPhone) {
        this.newPhone = newPhone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
