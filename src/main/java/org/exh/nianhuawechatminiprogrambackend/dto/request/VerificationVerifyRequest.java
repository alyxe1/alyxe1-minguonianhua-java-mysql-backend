package org.exh.nianhuawechatminiprogrambackend.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 核销验证请求
 */
@Data
@ApiModel("核销验证请求")
public class VerificationVerifyRequest {

    /**
     * 核销码
     */
    @NotBlank(message = "核销码不能为空")
    @ApiModelProperty(value = "核销码", required = true, example = "A1B2C3D4")
    private String code;
}
