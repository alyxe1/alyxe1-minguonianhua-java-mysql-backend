package org.exh.nianhuawechatminiprogrambackend.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
 import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import org.exh.nianhuawechatminiprogrambackend.config.OssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 核销码工具类
 * 用于生成核销码、生成二维码并上传到OSS
 */
@Component
public class VerificationCodeUtil {
    private static final Logger log = LoggerFactory.getLogger(VerificationCodeUtil.class);

    @Autowired
    private OssConfig ossConfig;

    /**
     * 生成核销码
     * @return 核销码（8位大写字母+数字）
     */
    public String generateVerificationCode() {
        // 生成8位大写字母+数字的核销码
        return RandomUtil.randomStringUpper(8);
    }

    /**
     * 生成二维码并上传到OSS
     * @param content 二维码内容（核销码）
     * @return OSS访问URL
     */
    public String generateQrCodeAndUpload(String content) {
        try {
            log.info("开始生成二维码并上传到OSS, content={}", content);

            // 1. 生成二维码配置
            QrConfig qrConfig = new QrConfig(300, 300);
            qrConfig.setMargin(2);

            // 2. 生成二维码到字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            QrCodeUtil.generate(content, qrConfig, "jpg", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            log.info("二维码生成成功, size={} bytes", qrCodeBytes.length);

            // 3. 构建OSS文件路径
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uuid = IdUtil.fastSimpleUUID();
            String fileName = String.format("qrcode_%s_%s.jpg", timestamp, uuid);
            String ossPath = "nianhua/verification/" + fileName;

            log.info("OSS上传路径: {}", ossPath);

            // 4. 上传到阿里云OSS
            OSS ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );

            try {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        ossConfig.getBucketName(),
                        ossPath,
                        new ByteArrayInputStream(qrCodeBytes)
                );

                // 设置Content-Type
                putObjectRequest.setProcess("image/jpg");

                ossClient.putObject(putObjectRequest);
                log.info("二维码上传到OSS成功");

            } finally {
                ossClient.shutdown();
            }

            // 5. 返回访问URL
            String fileUrl = ossConfig.getBucketDomain() + "/" + ossPath;
            log.info("二维码OSS访问URL: {}", fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("生成二维码并上传到OSS失败", e);
            throw new RuntimeException("生成二维码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成核销码并创建二维码上传到OSS
     * @return 包含核销码和二维码URL的对象
     */
    public VerificationCodeResult generateVerificationCodeWithQrCode() {
        // 1. 生成核销码
        String code = generateVerificationCode();
        log.info("生成核销码: {}", code);

        // 2. 生成二维码并上传
        String qrCodeUrl = generateQrCodeAndUpload(code);

        // 3. 返回结果
        VerificationCodeResult result = new VerificationCodeResult();
        result.setCode(code);
        result.setQrCodeUrl(qrCodeUrl);

        return result;
    }

    /**
     * 核销码生成结果
     */
    public static class VerificationCodeResult {
        private String code;
        private String qrCodeUrl;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getQrCodeUrl() {
            return qrCodeUrl;
        }

        public void setQrCodeUrl(String qrCodeUrl) {
            this.qrCodeUrl = qrCodeUrl;
        }
    }
}
