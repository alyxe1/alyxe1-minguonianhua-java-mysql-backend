package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信登录请求DTO
 */
@Data
public class WechatLoginRequest {

    @NotBlank(message = "code不能为空")
    private String code;

    private String nickname;

    private String avatarUrl;
}
