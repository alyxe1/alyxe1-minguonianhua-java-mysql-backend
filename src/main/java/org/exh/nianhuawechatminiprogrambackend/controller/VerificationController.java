package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.VerificationConfirmRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.VerificationVerifyRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.VerificationConfirmResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.VerificationVerifyResult;
import org.exh.nianhuawechatminiprogrambackend.service.VerificationCodeService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 核销控制器
 * 处理核销验证和核销确认请求
 */
@Api(tags = "核销模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

    @Autowired
    private VerificationCodeService verificationCodeService;

    @ApiOperation(value = "核销验证", notes = "管理员扫描二维码获得code后，调用此接口查询订单和预订信息进行核对，不做实际核销")
    @PostMapping("/verify")
    public Result<VerificationVerifyResult> verify(@Valid @RequestBody VerificationVerifyRequest request) {

        log.info("核销验证请求, code={}", request.getCode());

        try {
            // 调用 Service 层进行核销验证
            VerificationVerifyResult result = verificationCodeService.verify(request.getCode());
            return Result.success(result);
        } catch (Exception e) {
            log.error("核销验证失败, code={}", request.getCode(), e);
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "核销确认", notes = "管理员核对信息无误后，点击确认核销，实际更新核销码状态为已使用")
    @PostMapping("/confirm")
    public Result<VerificationConfirmResponse> confirm(@Valid @RequestBody VerificationConfirmRequest request) {

        log.info("核销确认请求, code={}, remarks={}", request.getCode(), request.getRemarks());

        try {
            // TODO: 从认证信息中获取当前管理员ID
            // 临时使用0作为adminId，实际应从JWT token中解析
            Long adminId = 0L;

            // 调用 Service 层进行核销确认
            VerificationConfirmResponse response = verificationCodeService.confirm(
                    request.getCode(),
                    request.getRemarks(),
                    adminId
            );
            return Result.success(response);
        } catch (Exception e) {
            log.error("核销确认失败, code={}", request.getCode(), e);
            return Result.error(e.getMessage());
        }
    }
}
