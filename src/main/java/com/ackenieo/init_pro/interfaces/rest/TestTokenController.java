package com.ackenieo.init_pro.interfaces.rest;

import com.ackenieo.init_pro.infrastructure.security.JwtTokenProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 测试用Token生成接口（仅dev环境）
 */
@RestController
@RequestMapping("/api/test")
@Profile("dev")
public class TestTokenController {

    private final JwtTokenProvider jwtTokenProvider;

    public TestTokenController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/generate-token")
    public Map<String, String> generateToken(@RequestParam(defaultValue = "test-user") String userId,
                                              @RequestParam(defaultValue = "13800138000") String phone) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, phone);
        return Map.of(
                "accessToken", accessToken,
                "userId", userId
        );
    }
}
