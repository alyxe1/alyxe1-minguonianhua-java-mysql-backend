package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 核销验证结果
 * 用于核销验证接口返回的数据结构
 */
@Data
@ApiModel("核销验证结果")
public class VerificationVerifyResult {

    @ApiModelProperty(value = "订单核销信息", required = true)
    private OrderVerificationInfo orderInfo;
}
