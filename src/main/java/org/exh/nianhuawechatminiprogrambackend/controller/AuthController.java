package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.WechatLoginRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.TokenRefreshResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.UserInfoResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.WechatLoginResponse;
import org.exh.nianhuawechatminiprogrambackend.service.AuthService;
import org.exh.nianhuawechatminiprogrambackend.utils.JwtUtil;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证控制器
 */
@Api(tags = "认证模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @ApiOperation("微信登录")
    @PostMapping("/wechat-login")
    public Result<WechatLoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        log.info("微信登录接口调用");
        WechatLoginResponse response = authService.wechatLogin(request);
        return Result.success(response);
    }

    @ApiOperation("刷新Token")
    @PostMapping("/refresh")
    public Result<TokenRefreshResponse> refreshToken(@RequestHeader("Authorization") String authorization) {
        log.info("Token刷新接口调用");

        // 从Authorization头中提取token
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Authorization头格式错误");
            return Result.error("无效的Authorization头");
        }

        String token = authorization.substring(7);

        // 验证token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token验证失败");
            return Result.error("Token无效或已过期");
        }

        // 从token中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);

        // 生成新的token
        TokenRefreshResponse response = authService.refreshToken(userId, token);
        return Result.success(response);
    }

    /**
     * 【临时调试接口】模拟微信登录，仅用于开发和测试
     * 不走真实微信API，但其他逻辑与正式微信登录一致（包括数据库交互）
     */
    @ApiOperation(value = "【临时调试】模拟微信登录", notes = "仅用于开发和测试环境，不走真实微信API，但其他逻辑与正式微信登录一致")
    @PostMapping("/mock-login")
    public Result<WechatLoginResponse> mockLogin(@RequestBody WechatLoginRequest request) {
        log.warn("【临时调试】调用模拟登录接口，仅用于开发测试");

        // 调用AuthService的模拟登录方法，逻辑与正式微信登录一致
        WechatLoginResponse response = authService.mockWechatLogin(request);
        return Result.success(response);
    }

    /**
     * 测试接口 - 获取当前登录用户信息
     * 用于验证JWT拦截器是否正常工作
     */
    @ApiOperation(value = "获取当前用户信息", notes = "需要JWT认证，用于测试拦截器")
    @GetMapping("/current-user")
    public Result<UserInfoResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("获取当前用户信息, userId: {}", userId);

        if (userId == null) {
            return Result.error("未获取到用户信息");
        }

        // 这里只是演示如何从request获取userId
        // 实际应用中应该查询数据库获取完整用户信息
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(userId);
        userInfo.setNickname("当前用户");
        userInfo.setRole("user");

        return Result.success(userInfo);
    }
}
