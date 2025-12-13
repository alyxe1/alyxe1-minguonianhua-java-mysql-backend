package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.UpdateAvatarAndNicknameRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.WechatLoginRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.*;
import org.exh.nianhuawechatminiprogrambackend.entity.User;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.UserMapper;
import org.exh.nianhuawechatminiprogrambackend.service.AuthService;
import org.exh.nianhuawechatminiprogrambackend.utils.JwtUtil;
import org.exh.nianhuawechatminiprogrambackend.utils.WechatApiUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WechatApiUtil wechatApiUtil;

    @Override
    public WechatLoginResponse wechatLogin(WechatLoginRequest request) {
        log.info("微信登录开始, code: {}", request.getCode());

        // 1. 调用微信接口获取session
        WechatSessionResponse sessionResponse = wechatApiUtil.code2Session(request.getCode());

        // 2. 检查微信返回的错误
        if (sessionResponse.getErrcode() != null && sessionResponse.getErrcode() != 0) {
            log.error("微信登录失败, errcode: {}, errmsg: {}", sessionResponse.getErrcode(), sessionResponse.getErrmsg());
            throw new BusinessException(ResultCode.ERROR, "微信登录失败: " + sessionResponse.getErrmsg());
        }

        // 3. 检查必要的返回参数
        if (sessionResponse.getOpenid() == null) {
            log.error("微信登录返回openid为空");
            throw new BusinessException(ResultCode.ERROR, "微信登录返回参数异常");
        }

        String openid = sessionResponse.getOpenid();
        String unionid = sessionResponse.getUnionid();
        String sessionKey = sessionResponse.getSessionKey();

        log.info("微信登录成功, openid: {}, unionid: {}", openid, unionid);

        // 4. 查询用户是否已存在
        User user = userMapper.selectByOpenid(openid);

        if (user == null) {
            // 5. 新用户，创建用户记录
            log.info("新用户，创建用户记录");
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(unionid != null ? unionid : "");
            user.setNickname(request.getNickname() != null ? request.getNickname() : "");
            user.setAvatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : "");
            user.setPhone("");
            user.setRole("user");
            user.setIsDeleted(0);

            userMapper.insert(user);
            log.info("用户创建成功, userId: {}", user.getId());
        } else {
            // 6. 已存在用户，更新信息
            log.info("用户已存在，更新用户信息, userId: {}", user.getId());
            boolean needUpdate = false;

            if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
                user.setNickname(request.getNickname());
                needUpdate = true;
            }

            if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(request.getAvatarUrl());
                needUpdate = true;
            }

            if (needUpdate) {
                userMapper.updateById(user);
                log.info("用户信息更新成功");
            }
        }

        // 7. 生成token
        String accessToken = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        log.info("Token生成成功, userId: {}", user.getId());

        // 8. 构建响应
        WechatLoginResponse response = new WechatLoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400); // 24小时
        response.setOpenid(openid);
        response.setUnionid(unionid != null ? unionid : "");

        // 用户信息
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(user.getId());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatarUrl());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole());
        response.setUserInfo(userInfo);

        log.info("微信登录完成, userId: {}", user.getId());
        return response;
    }

    @Override
    public TokenRefreshResponse refreshToken(Long userId, String refreshToken) {
        log.info("Token刷新开始, userId: {}", userId);

        // 1. 验证refreshToken
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("RefreshToken验证失败, userId: {}", userId);
            throw new BusinessException(ResultCode.TOKEN_INVALID, "RefreshToken无效或已过期");
        }

        // 2. 验证token中的用户ID是否匹配
        Long tokenUserId = jwtUtil.getUserIdFromToken(refreshToken);
        if (!tokenUserId.equals(userId)) {
            log.warn("Token用户ID不匹配, tokenUserId: {}, requestUserId: {}", tokenUserId, userId);
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token用户不匹配");
        }

        // 3. 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在, userId: {}", userId);
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 4. 生成新的token
        String newAccessToken = jwtUtil.generateToken(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        log.info("Token刷新成功, userId: {}", userId);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken, "Bearer", 86400); // 24小时
    }

    @Override
    public WechatLoginResponse mockWechatLogin(WechatLoginRequest request) {
        log.warn("【临时调试】模拟微信登录开始");

        // 生成模拟的OpenID和UnionID（基于昵称或固定生成，确保同一用户每次登录返回相同的OpenID）
        String mockOpenid = generateMockOpenid(request.getNickname());
        String mockUnionid = generateMockUnionid(mockOpenid);

        log.info("模拟微信登录 - openid: {}, unionid: {}", mockOpenid, mockUnionid);

        // 查询用户是否已存在（逻辑与正式微信登录一致）
        User user = userMapper.selectByOpenid(mockOpenid);

        if (user == null) {
            // 新用户，创建用户记录
            log.info("模拟登录 - 新用户，创建用户记录");
            user = new User();
            user.setOpenid(mockOpenid);
            user.setUnionid(mockUnionid);
            user.setNickname(request.getNickname() != null ? request.getNickname() : "测试用户");
            user.setAvatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : "https://example.com/avatar.jpg");
            user.setPhone("");
            user.setRole("user");
            user.setIsDeleted(0);

            userMapper.insert(user);
            log.info("模拟登录 - 用户创建成功, userId: {}", user.getId());
        } else {
            // 已存在用户，更新信息
            log.info("模拟登录 - 用户已存在，更新用户信息, userId: {}", user.getId());
            boolean needUpdate = false;

            if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
                user.setNickname(request.getNickname());
                needUpdate = true;
            }

            if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(request.getAvatarUrl());
                needUpdate = true;
            }

            if (needUpdate) {
                userMapper.updateById(user);
                log.info("模拟登录 - 用户信息更新成功");
            }
        }

        // 生成token（逻辑与正式微信登录一致）
        String accessToken = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        log.info("模拟登录 - Token生成成功, userId: {}", user.getId());

        // 构建响应（逻辑与正式微信登录一致）
        WechatLoginResponse response = new WechatLoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400); // 24小时
        response.setOpenid(mockOpenid);
        response.setUnionid(mockUnionid);

        // 用户信息
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(user.getId());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatarUrl());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole());
        response.setUserInfo(userInfo);

        log.info("模拟登录完成, userId: {}", user.getId());
        return response;
    }

    /**
     * 根据昵称生成模拟OpenID
     */
    private String generateMockOpenid(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "mock_openid_default_user";
        }
        return "mock_openid_" + nickname.hashCode();
    }

    /**
     * 根据OpenID生成模拟UnionID
     */
    private String generateMockUnionid(String openid) {
        // 缩短前缀，确保总长度不超过32个字符（数据库限制）
        return "m_" + openid.substring(0, Math.min(30, openid.length()));
    }

    @Override
    public String updateAvatarAndNickname(UpdateAvatarAndNicknameRequest request) {
        log.info("修改用户头像和昵称，userId={}", request.getUserId());

        // 1. 验证用户ID
        Long userId;
        try {
            userId = Long.valueOf(request.getUserId());
        } catch (NumberFormatException e) {
            log.error("用户ID格式错误，userId={}", request.getUserId());
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID格式错误");
        }

        // 2. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("用户不存在，userId={}", userId);
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 3. 检查是否需要更新
        boolean needUpdate = false;

        // 更新头像
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
            needUpdate = true;
            log.info("更新头像，userId={}", userId);
        }

        // 更新昵称
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            user.setNickname(request.getNickname());
            needUpdate = true;
            log.info("更新昵称，userId={}", userId);
        }

        // 4. 执行更新
        if (needUpdate) {
            userMapper.updateById(user);
            log.info("用户头像和昵称更新成功，userId={}", userId);
        } else {
            log.info("用户信息无需更新，userId={}", userId);
        }

        return "success";
    }
}
