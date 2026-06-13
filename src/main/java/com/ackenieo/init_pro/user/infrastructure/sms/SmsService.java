package com.ackenieo.init_pro.user.infrastructure.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务
 */
@Service
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.sms.url}")
    private String smsUrl;
    @Value("${app.sms.product-name}")
    private String productName;

    public SmsService(StringRedisTemplate redisTemplate, RestTemplate restTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    public void sendCode(String phone) {
        String code = generateCode();
        String key = CODE_KEY_PREFIX + phone;

        redisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        sendSms(phone, code);
        log.info("\u9a8c\u8bc1\u7801\u5df2\u751f\u6210\u5e76\u53d1\u9001: phone={}, code={}", phone, code);
    }

    public void verifyCode(String phone, String code) {
        String key = CODE_KEY_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new RuntimeException("\u9a8c\u8bc1\u7801\u5df2\u8fc7\u671f");
        }

        if (!storedCode.equals(code)) {
            throw new RuntimeException("\u9a8c\u8bc1\u7801\u9519\u8bef");
        }

        redisTemplate.delete(key);
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private void sendSms(String phone, String code) {
        try {
            String url = smsUrl + "?name=" + productName + "&code=" + code + "&number=" + CODE_EXPIRE_MINUTES + "&to=" + phone;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("短信发送失败(HTTP): phone={}, status={}", phone, response.getStatusCode());
                return;
            }

            String body = response.getBody();
            JsonNode json = objectMapper.readTree(body);
            int bizCode = json.get("code").asInt();

            if (bizCode == 200 || bizCode == 0) {
                log.info("短信发送成功: phone={}", phone);
            } else {
                log.warn("短信发送失败(业务): phone={}, code={}, msg={}", phone, bizCode, json.get("msg").asText());
            }
        } catch (Exception e) {
            log.error("\u77ed\u4fe1\u53d1\u9001\u5f02\u5e38: phone={}", phone, e);
        }
    }
}
