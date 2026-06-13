package com.ackenieo.init_pro.user.infrastructure.security;

import com.ackenieo.init_pro.user.domain.entity.User;
import com.ackenieo.init_pro.user.domain.service.UserDomainServiceImpl;
import com.ackenieo.init_pro.user.infrastructure.sms.SmsService;
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

    public void sendSmsCode(String phone) {
        validatePhone(phone);
        smsService.sendCode(phone);
        log.info("\u53d1\u9001\u9a8c\u8bc1\u7801\u5230\u624b\u673a\u53f7: {}", phone);
    }

    public Map<String, String> login(String phone, String code) {
        validatePhone(phone);
        smsService.verifyCode(phone, code);

        User user = userDomainService.findByPhone(phone)
                .orElseGet(() -> userDomainService.createUser(phone));

        return generateTokens(user);
    }

    public Map<String, String> refreshToken(String refreshToken) {
        String phone = jwtTokenProvider.validateRefreshToken(refreshToken);
        User user = userDomainService.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("\u7528\u6237\u4e0d\u5b58\u5728"));

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
            throw new RuntimeException("\u624b\u673a\u53f7\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
    }
}
