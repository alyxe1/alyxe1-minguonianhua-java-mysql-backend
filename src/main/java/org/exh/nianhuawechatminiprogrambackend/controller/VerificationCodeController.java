package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.entity.VerificationCode;
import org.exh.nianhuawechatminiprogrambackend.service.VerificationCodeService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 核销码控制器
 */
@Api(tags = "核销模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/verification-codes")
public class VerificationCodeController {

    @Autowired
    private VerificationCodeService verificationCodeService;

    @ApiOperation(value = "获取核销码", notes = "用户进入订单详情页后点击核销二维码，通过order_no获取核销码和二维码URL")
    @GetMapping
    public Result<VerificationCode> getVerificationCode(
            @ApiParam(value = "订单号", required = true, example = "202511291235001234")
            @RequestParam("orderNo") Long orderNo) {

        log.info("获取核销码请求, orderNo={}", orderNo);

        try {
            VerificationCode verificationCode = verificationCodeService.getVerificationCodeByOrderNo(String.valueOf(orderNo));
            return Result.success(verificationCode);
        } catch (Exception e) {
            log.error("获取核销码失败, orderNo={}", orderNo, e);
            return Result.error(e.getMessage());
        }
    }
}
