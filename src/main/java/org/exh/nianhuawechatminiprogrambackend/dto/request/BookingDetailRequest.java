package org.exh.nianhuawechatminiprogrambackend.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("预订详情请求")
public class BookingDetailRequest {

    @ApiModelProperty(value = "预订ID", required = true)
    @NotBlank(message = "预订ID不能为空")
    private String bookingId;

    @ApiModelProperty(value = "用户ID", required = true)
    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
