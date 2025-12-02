package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单核销信息
 * 用于核销验证接口返回的订单信息
 */
@Data
@ApiModel("订单核销信息")
public class OrderVerificationInfo {

    @ApiModelProperty(value = "订单号", example = "202511291235001234")
    private String orderNo;

    @ApiModelProperty(value = "主题名称", example = "民国年华 - 上海滩之夜")
    private String themeTitle;

    @ApiModelProperty(value = "订单金额（元）", example = "198.00")
    private Double amount;

    @ApiModelProperty(value = "预订人数", example = "2")
    private Integer peopleCount;

    @ApiModelProperty(value = "联系人姓名", example = "张三")
    private String contactName;

    @ApiModelProperty(value = "联系人电话", example = "13800138000")
    private String contactPhone;

    @ApiModelProperty(value = "场次时间", example = "2025-12-03 19:00:00")
    private LocalDateTime sessionTime;
}
