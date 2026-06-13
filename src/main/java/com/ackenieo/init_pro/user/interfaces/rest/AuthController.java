package com.ackenieo.init_pro.user.interfaces.rest;

import com.ackenieo.init_pro.shared.infrastructure.ApiResponse;
import com.ackenieo.init_pro.shared.infrastructure.BaseController;
import com.ackenieo.init_pro.user.infrastructure.security.AuthService;
import com.ackenieo.init_pro.user.interfaces.dto.LoginRequest;
import com.ackenieo.init_pro.user.interfaces.dto.RefreshTokenRequest;
import com.ackenieo.init_pro.user.interfaces.dto.SendSmsRequest;
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

    @PostMapping("/sms/send")
    public ResponseEntity<ApiResponse<Void>> sendSms(@RequestBody SendSmsRequest request) {
        authService.sendSmsCode(request.getPhone());
        return success(null);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        Map<String, String> tokens = authService.login(request.getPhone(), request.getCode());
        return success(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody RefreshTokenRequest request) {
        Map<String, String> tokens = authService.refreshToken(request.getRefreshToken());
        return success(tokens);
    }
}
