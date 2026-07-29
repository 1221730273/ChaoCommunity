package com.ljc.chaocommunity.util;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.CopyObjectRequest;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传工具类
 */
@Component
public class OssUtil {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.region}")
    private String region;

    @Value("${oss.bucket-name}")
    private String bucketName;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    private OSSClient client;

    @PostConstruct
    public void init() {
        client = OSSClient.newBuilder()
                .endpoint(endpoint)
                .region(region)
                .credentialsProvider(new StaticCredentialsProvider(accessKeyId, accessKeySecret))
                .build();
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 上传文件到 OSS
     *
     * @param file   上传的文件
     * @param folder 存储目录（如 avatar、post/cover）
     * @return 上传结果（包含 objectKey 和 url）
     */
    public UploadResult upload(MultipartFile file, String folder) {
        // 1. 生成唯一文件名：目录/日期/uuid.扩展名
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = folder + "/" + dateStr + "/" + UUID.randomUUID() + extension;

        // 2. 上传
        try {
            PutObjectRequest request = PutObjectRequest.newBuilder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .body(BinaryData.fromStream(file.getInputStream(), file.getSize()))
                    .contentType(file.getContentType())
                    .build();
            client.putObject(request);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }

        // 3. 拼接访问 URL
        String url = endpoint + "/" + objectKey;
        return new UploadResult(objectKey, url);
    }

    /**
     * 上传结果
     */
    public record UploadResult(String objectKey, String url) {}

    /**
     * 移动文件到另一个位置（同 bucket 内复制后删除源文件）
     *
     * @param sourceKey 源文件路径
     * @param targetKey 目标文件路径
     * @return 新的文件访问 URL
     */
    public UploadResult move(String sourceKey, String targetKey) {
        // 1. 复制到新位置
        CopyObjectRequest copyRequest = CopyObjectRequest.newBuilder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .bucket(bucketName)
                .key(targetKey)
                .build();
        client.copyObject(copyRequest);

        // 2. 删除源文件
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.newBuilder()
                .bucket(bucketName)
                .key(sourceKey)
                .build();
        client.deleteObject(deleteRequest);

        String url = endpoint + "/" + targetKey;
        return new UploadResult(targetKey, url);
    }

    /**
     * 删除 OSS 上的文件
     *
     * @param objectKey 文件路径（如 avatar/2025-01-01/xxx.jpg）
     */
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.newBuilder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        client.deleteObject(request);
    }
}
