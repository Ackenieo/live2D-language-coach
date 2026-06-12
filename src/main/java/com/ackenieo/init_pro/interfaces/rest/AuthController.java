package com.ackenieo.init_pro.interfaces.rest;

import com.ackenieo.init_pro.application.service.AuthService;
import com.ackenieo.init_pro.interfaces.base.BaseController;
import com.ackenieo.init_pro.interfaces.dto.ApiResponse;
import com.ackenieo.init_pro.interfaces.dto.LoginRequest;
import com.ackenieo.init_pro.interfaces.dto.RefreshTokenRequest;
import com.ackenieo.init_pro.interfaces.dto.SendSmsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 发送验证码
     */
    @PostMapping("/sms/send")
    public ResponseEntity<ApiResponse<Void>> sendSms(@RequestBody SendSmsRequest request) {
        authService.sendSmsCode(request.getPhone());
        return success(null);
    }

    /**
     * 手机号+验证码登录（不存在则自动注册）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        Map<String, String> tokens = authService.login(request.getPhone(), request.getCode());
        return success(tokens);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody RefreshTokenRequest request) {
        Map<String, String> tokens = authService.refreshToken(request.getRefreshToken());
        return success(tokens);
    }
}
