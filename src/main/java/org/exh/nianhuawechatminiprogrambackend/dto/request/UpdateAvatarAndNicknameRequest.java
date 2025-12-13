package org.exh.nianhuawechatminiprogrambackend.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 修改头像和昵称请求DTO
 */
@Data
@ApiModel("修改头像和昵称请求")
public class UpdateAvatarAndNicknameRequest {

    /**
     * 用户ID，对应数据库users表的id字段
     */
    @NotNull(message = "用户ID不能为空")
    @ApiModelProperty(value = "用户ID，对应数据库users表的id字段", required = true)
    private String userId;

    /**
     * 头像URL，对应数据库users表的avatar_url字段
     */
    @ApiModelProperty(value = "头像URL，对应数据库users表的avatar_url字段")
    private String avatarUrl;

    /**
     * 昵称，对应数据库users表的nickname字段
     */
    @ApiModelProperty(value = "昵称，对应数据库users表的nickname字段")
    private String nickname;
}
