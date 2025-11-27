package org.exh.nianhuawechatminiprogrambackend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface UploadService {

    /**
     * 上传文件到OSS
     * @param file 要上传的文件
     * @param directory 存储目录（如avatar、booking、verification等）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String directory);
}
