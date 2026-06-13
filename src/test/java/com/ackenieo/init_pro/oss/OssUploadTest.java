package com.ackenieo.init_pro.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * OSS 上传测试脚本
 *
 * Bucket: english-ai-coach
 * Endpoint: oss-cn-beijing.aliyuncs.com
 * 地域: cn-beijing (华北2)
 * 读写权限: 私有（需签名 URL 访问）
 */
public class OssUploadTest {

    private static final String ENDPOINT = "oss-cn-beijing.aliyuncs.com";
    private static final String ACCESS_KEY_ID = "LTAI5t5hUsaXhDmRHDEdtuVk";
    private static final String ACCESS_KEY_SECRET = "s37vZ8tywq5FILw6c30AgBXvTv1vhl";
    private static final String BUCKET_NAME = "english-ai-coach";

    // 本地图片路径
    private static final String LOCAL_FILE = "C:\\Users\\13435\\Desktop\\193cdabd59877c1acc11cef78e04d12b.jpg";

    // OSS 存放路径
    private static final String OBJECT_NAME = "avatar/test-upload.jpg";

    public static void main(String[] args) {
        OSS ossClient = null;
        try {
            // 1. 创建 OSS 客户端
            ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
            System.out.println("[OK] OSS client created, endpoint=" + ENDPOINT);

            // 2. 检查文件
            File file = new File(LOCAL_FILE);
            if (!file.exists()) {
                System.err.println("[FAIL] File not found: " + LOCAL_FILE);
                return;
            }
            System.out.println("[OK] File ready, size=" + file.length() + " bytes");

            // 3. 上传
            try (InputStream inputStream = new FileInputStream(file)) {
                PutObjectResult result = ossClient.putObject(BUCKET_NAME, OBJECT_NAME, inputStream);
                System.out.println("[OK] Upload success, ETag=" + result.getETag());
            }

            // 4. 签名 URL（Bucket 私有，必须签名才能访问）
            Date expiration = new Date(System.currentTimeMillis() + 3600_000);
            URL signedUrl = ossClient.generatePresignedUrl(BUCKET_NAME, OBJECT_NAME, expiration);
            System.out.println("[OK] Signed URL (valid 1 hour):");
            System.out.println(signedUrl);

            // 5. 原始 URL
            String rawUrl = "https://" + BUCKET_NAME + "." + ENDPOINT + "/" + OBJECT_NAME;
            System.out.println("[INFO] Raw URL (private, no direct access): " + rawUrl);

            // 6. 确认文件存在
            boolean exists = ossClient.doesObjectExist(BUCKET_NAME, OBJECT_NAME);
            System.out.println("[OK] Verify object exists: " + exists);

        } catch (Exception e) {
            System.err.println("[FAIL] " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
                System.out.println("[OK] OSS client closed");
            }
        }
    }
}
