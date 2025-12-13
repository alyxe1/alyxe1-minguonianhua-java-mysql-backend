package org.exh.nianhuawechatminiprogrambackend.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@ApiModel("用户预订列表请求")
public class UserBookingListRequest {

    @ApiModelProperty(value = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 100, message = "每页数量最大值为100")
    private Integer pageSize = 10;

    @ApiModelProperty(value = "状态筛选", allowableValues = "pending,paid,cancelled,refunded,completed")
    private String status;

    // 计算偏移量
    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
