package com.ackenieo.init_pro.user.interfaces.rest;

import com.ackenieo.init_pro.shared.infrastructure.ApiResponse;
import com.ackenieo.init_pro.shared.infrastructure.BaseController;
import com.ackenieo.init_pro.user.application.service.UserProfileAppService;
import com.ackenieo.init_pro.user.infrastructure.security.CurrentUserConstants;
import com.ackenieo.init_pro.user.interfaces.dto.AvatarUploadResponse;
import com.ackenieo.init_pro.user.interfaces.dto.ChangePhoneRequest;
import com.ackenieo.init_pro.user.interfaces.dto.UpdateProfileRequest;
import com.ackenieo.init_pro.user.interfaces.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户资料控制器
 */
@RestController
@RequestMapping("/api/user")
public class ProfileController extends BaseController {

    private final UserProfileAppService userProfileAppService;

    public ProfileController(UserProfileAppService userProfileAppService) {
        this.userProfileAppService = userProfileAppService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(HttpServletRequest request) {
        return success(userProfileAppService.getProfile(currentUserId(request)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(HttpServletRequest request,
                                                                          @RequestBody UpdateProfileRequest updateProfileRequest) {
        return success(userProfileAppService.updateProfile(currentUserId(request), updateProfileRequest));
    }

    @PutMapping("/phone")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePhone(HttpServletRequest request,
                                                                        @RequestBody ChangePhoneRequest changePhoneRequest) {
        return success(userProfileAppService.changePhone(currentUserId(request), changePhoneRequest));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<AvatarUploadResponse>> uploadAvatar(HttpServletRequest request,
                                                                          @RequestParam("file") MultipartFile file) {
        return success(userProfileAppService.updateAvatar(currentUserId(request), file));
    }

    private String currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(CurrentUserConstants.CURRENT_USER_ID);
        if (userId == null) {
            throw new RuntimeException("当前用户未登录");
        }
        return String.valueOf(userId);
    }
}
