package org.exh.nianhuawechatminiprogrambackend.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 核销确认请求
 */
@Data
@ApiModel("核销确认请求")
public class VerificationConfirmRequest {

    /**
     * 核销码
     */
    @NotBlank(message = "核销码不能为空")
    @ApiModelProperty(value = "核销码", required = true, example = "A1B2C3D4")
    private String code;

    /**
     * 核销备注
     */
    @ApiModelProperty(value = "核销备注", example = "用户已入场")
    private String remarks;
}
