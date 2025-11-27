package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.Data;

/**
 * 微信登录响应DTO
 */
@Data
public class WechatLoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
    private UserInfoResponse userInfo;
    private String openid;
    private String unionid;
}
