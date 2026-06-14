package com.ackenieo.init_pro.oss.infrastructure;

import com.ackenieo.init_pro.oss.domain.OssClient;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 客户端实现
 */
@Component
public class AliyunOssClient implements OssClient {
    private static final long DEFAULT_EXPIRE_MILLIS = 3600_000L;

    private final OSS oss;
    private final OssConfig ossConfig;

    public AliyunOssClient(OSS oss, OssConfig ossConfig) {
        this.oss = oss;
        this.ossConfig = ossConfig;
    }

    @Override
    public String upload(byte[] data, String objectKey, String contentType) {
        if (!ossConfig.isEnabled()) {
            throw new RuntimeException("OSS未启用");
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        metadata.setContentType(contentType);
        oss.putObject(ossConfig.getBucketName(), objectKey, new ByteArrayInputStream(data), metadata);

        Date expiration = new Date(System.currentTimeMillis() + DEFAULT_EXPIRE_MILLIS);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossConfig.getBucketName(), objectKey);
        request.setExpiration(expiration);
        URL url = oss.generatePresignedUrl(request);
        return url.toString();
    }
}
