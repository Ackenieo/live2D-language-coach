package com.ackenieo.init_pro.application.service;

import com.ackenieo.init_pro.domain.model.entity.User;
import com.ackenieo.init_pro.domain.service.impl.UserDomainServiceImpl;
import com.ackenieo.init_pro.infrastructure.external.SmsService;
import com.ackenieo.init_pro.infrastructure.security.JwtTokenProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 认证应用服务
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDomainServiceImpl userDomainService;
    private final SmsService smsService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserDomainServiceImpl userDomainService,
                       SmsService smsService,
                       JwtTokenProvider jwtTokenProvider) {
        this.userDomainService = userDomainService;
        this.smsService = smsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 发送验证码
     */
    public void sendSmsCode(String phone) {
        validatePhone(phone);
        smsService.sendCode(phone);
        log.info("发送验证码到手机号: {}", phone);
    }

    /**
     * 手机号+验证码登录（不存在则自动注册）
     */
    public Map<String, String> login(String phone, String code) {
        validatePhone(phone);
        smsService.verifyCode(phone, code);

        User user = userDomainService.findByPhone(phone)
                .orElseGet(() -> userDomainService.createUser(phone));

        return generateTokens(user);
    }

    /**
     * 刷新Token
     */
    public Map<String, String> refreshToken(String refreshToken) {
        String phone = jwtTokenProvider.validateRefreshToken(refreshToken);
        User user = userDomainService.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return generateTokens(user);
    }

    private Map<String, String> generateTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getPhone());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getPhone());
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", user.getId(),
                "phone", user.getPhone()
        );
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
    }
}
