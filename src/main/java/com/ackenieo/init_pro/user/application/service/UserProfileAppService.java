package com.ackenieo.init_pro.user.application.service;

import com.ackenieo.init_pro.oss.domain.OssClient;
import com.ackenieo.init_pro.user.domain.entity.User;
import com.ackenieo.init_pro.user.domain.repository.UserRepository;
import com.ackenieo.init_pro.user.infrastructure.security.JwtTokenProvider;
import com.ackenieo.init_pro.user.infrastructure.sms.SmsService;
import com.ackenieo.init_pro.user.interfaces.dto.AvatarUploadResponse;
import com.ackenieo.init_pro.user.interfaces.dto.ChangePhoneRequest;
import com.ackenieo.init_pro.user.interfaces.dto.UpdateProfileRequest;
import com.ackenieo.init_pro.user.interfaces.dto.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户资料应用服务
 */
@Service
public class UserProfileAppService {
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OssClient ossClient;

    public UserProfileAppService(UserRepository userRepository,
                                 SmsService smsService,
                                 JwtTokenProvider jwtTokenProvider,
                                 OssClient ossClient) {
        this.userRepository = userRepository;
        this.smsService = smsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.ossClient = ossClient;
    }

    public UserProfileResponse getProfile(String userId) {
        User user = requireUser(userId);
        return toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
        if (nickname.length() < 2 || nickname.length() > 16) {
            throw new RuntimeException("昵称长度需为2到16个字符");
        }
        user.setNickname(nickname);
        user.markUpdated();
        return toProfileResponse(userRepository.save(user));
    }

    public Map<String, String> changePhone(String userId, ChangePhoneRequest request) {
        User user = requireUser(userId);
        String newPhone = request.getNewPhone();
        validatePhone(newPhone);
        if (user.getPhone().equals(newPhone)) {
            throw new RuntimeException("新手机号不能与当前手机号相同");
        }
        if (userRepository.existsByPhone(newPhone)) {
            throw new RuntimeException("该手机号已被绑定");
        }
        smsService.verifyCode(newPhone, request.getCode());
        user.setPhone(newPhone);
        user.markUpdated();
        User saved = userRepository.save(user);
        return Map.of(
                "accessToken", jwtTokenProvider.generateAccessToken(saved.getId(), saved.getPhone()),
                "refreshToken", jwtTokenProvider.generateRefreshToken(saved.getId(), saved.getPhone()),
                "userId", saved.getId(),
                "phone", saved.getPhone()
        );
    }

    public AvatarUploadResponse updateAvatar(String userId, MultipartFile file) {
        User user = requireUser(userId);
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("头像文件不能超过5MB");
        }
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!contentType.startsWith("image/")) {
            throw new RuntimeException("仅支持图片类型头像");
        }
        String avatarUrl = uploadAvatarToOss(userId, file, contentType);
        user.setAvatarUrl(avatarUrl);
        user.markUpdated();
        userRepository.save(user);
        return new AvatarUploadResponse(avatarUrl);
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setPhone(maskPhone(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
    }

    private String uploadAvatarToOss(String userId, MultipartFile file, String contentType) {
        try {
            String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("avatar.png");
            String extension = ".png";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }
            String objectKey = "avatar/" + userId + "/" + UUID.randomUUID() + extension;
            return ossClient.upload(file.getBytes(), objectKey, contentType);
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败");
        }
    }
}
