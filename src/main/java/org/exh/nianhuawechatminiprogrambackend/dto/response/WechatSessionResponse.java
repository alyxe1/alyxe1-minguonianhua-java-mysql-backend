package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.Data;

/**
 * 微信session响应DTO
 */
@Data
public class WechatSessionResponse {
    private String openid;
    private String sessionKey;
    private String unionid;
    private Integer errcode;
    private String errmsg;
}
