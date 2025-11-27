package org.exh.nianhuawechatminiprogrambackend.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.config.OssConfig;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传服务实现类
 */
@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    @Autowired
    private OssConfig ossConfig;

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.ERROR, "上传文件不能为空");
        }

        // 文件大小限制（10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.ERROR, "文件大小不能超过10MB");
        }

        // 获取文件原始名称和后缀
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unknown";
        }
        String fileExtension = getFileExtension(originalFilename);

        // 生成存储路径
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = String.format("%s_%s%s", timestamp, uuid, fileExtension);

        // 构建存储路径
        String objectName;
        if (directory != null && !directory.isEmpty()) {
            objectName = String.format("%s/%s/%s", ossConfig.getBaseDir(), directory, fileName);
        } else {
            objectName = String.format("%s/%s", ossConfig.getBaseDir(), fileName);
        }

        InputStream inputStream = null;
        OSS ossClient = null;

        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );

            // 获取文件输入流
            inputStream = file.getInputStream();

            // 创建上传请求
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // 上传文件
            log.info("开始上传文件到OSS: objectName={}, bucket={}", objectName, ossConfig.getBucketName());
            ossClient.putObject(ossConfig.getBucketName(), objectName, inputStream, metadata);
            log.info("文件上传成功: objectName={}", objectName);

            // 生成访问URL
            String fileUrl = String.format("https://%s/%s", ossConfig.getBucketDomain(), objectName);
            log.info("文件访问URL: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.ERROR, "文件上传失败: " + e.getMessage());
        } finally {
            // 关闭资源
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("关闭输入流失败: {}", e.getMessage());
                }
            }
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 获取文件后缀名
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex);
        }
        // 默认后缀
        return ".jpg";
    }
}
