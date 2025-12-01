package org.exh.nianhuawechatminiprogrambackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssConfig {
    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * Endpoint
     */
    private String endpoint;

    /**
     * Bucket域名
     */
    private String bucketDomain;

    /**
     * Bucket名称
     */
    private String bucketName;

    /**
     * 文件存储根目录
     */
    private String baseDir = "nianhua";

    // 手动添加getter方法（Lombok配置问题）
    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getBucketDomain() {
        return bucketDomain;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBaseDir() {
        return baseDir;
    }
}
