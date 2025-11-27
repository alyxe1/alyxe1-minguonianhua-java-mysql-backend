package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.UploadFileResponse;
import org.exh.nianhuawechatminiprogrambackend.service.UploadService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Api(tags = "文件上传模块")
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @ApiOperation(value = "上传文件", notes = "上传文件到阿里云OSS\n\n本接口后端使用阿里OSS对象存储服务（通过bucket访问）\n通过Bucket访问OSS文档链接：https://help.aliyun.com/zh/oss/user-guide/access-oss-via-bucket-domain-name?spm=a2c4g.11186623.help-menu-31815.d_0_1_1.b1a76a75AB201P\n\n\n使用示例\n\n  上传头像：\n  curl --location --request POST 'http://localhost:8080/api/v1/uploadFile' \\  --header 'Authorization: Bearer your_jwt_token' \\  --form 'file=@\"/path/to/avatar.jpg\"'\n\n  通用文件上传：\n  curl --location --request POST 'http://localhost:8080/api/v1/uploadFile' \\  --header 'Authorization: Bearer your_jwt_token' \\  --form 'file=@\"/path/to/document.pdf\"' \\  --form 'directory=\"booking\"'\n\n\n\n文件存储路径\n\n  所有文件都存储在OSS的nianhua/目录下：\n  - 头像：nianhua/avatar/{timestamp}_{uuid}.jpg\n  - 预订凭证：nianhua/booking/{timestamp}_{uuid}.pdf\n  - 核销凭证：nianhua/verification/{timestamp}_{uuid}.jpg\n  - 其他：nianhua/other/{timestamp}_{uuid}.{ext}")
    @PostMapping(value = "/uploadFile", consumes = "multipart/form-data")
    public Result<UploadFileResponse> uploadFile(
            @ApiParam(value = "要上传的文件", required = true)
            @RequestPart("file") MultipartFile file,
            @ApiParam(value = "存储目录（如avatar、booking、verification等）")
            @RequestParam(value = "directory", required = false) String directory) {

        log.info("文件上传接口调用, fileName={}, directory={}",
                file != null ? file.getOriginalFilename() : "null",
                directory);

        try {
            // 调用服务层上传文件
            String fileUrl = uploadService.uploadFile(file, directory);

            // 构建响应
            UploadFileResponse response = new UploadFileResponse(fileUrl);

            log.info("文件上传成功, fileUrl={}", fileUrl);
            return Result.success(response);

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
