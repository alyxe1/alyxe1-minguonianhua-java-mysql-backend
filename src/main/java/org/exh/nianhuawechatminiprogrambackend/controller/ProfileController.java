package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.UpdateAvatarAndNicknameRequest;
import org.exh.nianhuawechatminiprogrambackend.service.AuthService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 用户资料控制器
 */
@Api(tags = "用户资料模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    @Autowired
    private AuthService authService;

    @ApiOperation(value = "修改头像和昵称", notes = "修改用户的头像URL和昵称信息")
    @PostMapping("/alterAvatarAndNickname")
    public Result<String> alterAvatarAndNickname(
            @ApiParam(value = "修改头像和昵称请求", required = true)
            @Valid @RequestBody UpdateAvatarAndNicknameRequest request,
            HttpServletRequest httpRequest) {

        log.info("修改头像和昵称接口调用，userId={}", request.getUserId());

        try {
            // 从JWT token中获取当前登录用户ID
            Long tokenUserId = (Long) httpRequest.getAttribute("userId");
            if (tokenUserId == null) {
                log.error("未获取到token中的用户ID");
                return Result.error("未登录或token无效");
            }

            // 验证请求中的userId是否与token中的userId一致
            if (!tokenUserId.toString().equals(request.getUserId())) {
                log.error("用户权限验证失败，tokenUserId={}, requestUserId={}", tokenUserId, request.getUserId());
                return Result.error("无权修改其他用户信息");
            }

            // 调用service修改头像和昵称
            String result = authService.updateAvatarAndNickname(request);

            log.info("修改头像和昵称成功，userId={}", request.getUserId());
            return Result.success(result);

        } catch (Exception e) {
            log.error("修改头像和昵称失败，userId={}", request.getUserId(), e);
            return Result.error("修改失败：" + e.getMessage());
        }
    }
}
