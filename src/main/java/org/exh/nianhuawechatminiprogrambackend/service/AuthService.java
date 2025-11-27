package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.request.WechatLoginRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.TokenRefreshResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.WechatLoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 微信登录
     * @param request 登录请求
     * @return 登录响应
     */
    WechatLoginResponse wechatLogin(WechatLoginRequest request);

    /**
     * 刷新Token
     * @param userId 用户ID
     * @param refreshToken 刷新令牌
     * @return Token刷新响应
     */
    TokenRefreshResponse refreshToken(Long userId, String refreshToken);

    /**
     * 模拟微信登录（仅用于开发和测试）
     * 不走真实微信API，但其他逻辑与正式微信登录一致
     * @param request 登录请求
     * @return 登录响应
     */
    WechatLoginResponse mockWechatLogin(WechatLoginRequest request);
}
