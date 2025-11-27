package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.Data;

/**
 * 用户信息响应DTO
 */
@Data
public class UserInfoResponse {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String role;
}
