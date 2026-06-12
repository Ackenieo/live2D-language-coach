package com.ackenieo.init_pro.infrastructure.external;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务
 */
@Service
@Slf4j
public class SmsService {
    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;

    private final RedissonClient redissonClient;
    private final RestTemplate restTemplate;

    @Value("${app.sms.url}")
    private String smsUrl;
    @Value("${app.sms.product-name}")
    private String productName;

    public SmsService(RedissonClient redissonClient, RestTemplate restTemplate) {
        this.redissonClient = redissonClient;
        this.restTemplate = restTemplate;
    }

    /**
     * 生成并发送验证码
     */
    public void sendCode(String phone) {
        String code = generateCode();
        String key = CODE_KEY_PREFIX + phone;

        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        sendSms(phone, code);
        log.info("验证码已生成并发送: phone={}, code={}", phone, code);
    }

    /**
     * 验证验证码
     */
    public void verifyCode(String phone, String code) {
        String key = CODE_KEY_PREFIX + phone;
        RBucket<String> bucket = redissonClient.getBucket(key);
        String storedCode = bucket.get();

        if (storedCode == null) {
            throw new RuntimeException("验证码已过期");
        }

        if (!storedCode.equals(code)) {
            throw new RuntimeException("验证码错误");
        }

        // 验证成功后删除验证码
        bucket.delete();
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    /**
     * 调用 Spug 短信验证码模板 API
     * 格式：POST https://push.spug.cc/send/模板ID?code=验证码&targets=手机号
     */
    private void sendSms(String phone, String code) {
        try {
            String url = smsUrl + "?name=" + productName + "&code=" + code + "&number=" + CODE_EXPIRE_MINUTES + "&to=" + phone;

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("短信发送失败: phone={}, status={}", phone, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("短信发送异常: phone={}", phone, e);
        }
    }
}
