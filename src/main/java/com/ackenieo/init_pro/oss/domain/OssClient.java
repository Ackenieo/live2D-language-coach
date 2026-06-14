package com.ackenieo.init_pro.oss.domain;

/**
 * OSS 客户端抽象
 */
public interface OssClient {
    String upload(byte[] data, String objectKey, String contentType);
}
