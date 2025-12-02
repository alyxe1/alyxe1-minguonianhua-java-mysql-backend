package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核销确认响应
 */
@Data
@ApiModel("核销确认响应")
public class VerificationConfirmResponse {

    @ApiModelProperty(value = "核销记录ID", example = "1")
    private Long verificationId;

    @ApiModelProperty(value = "核销码", example = "A1B2C3D4")
    private String code;

    @ApiModelProperty(value = "核销时间", example = "2025-12-02T20:00:00")
    private LocalDateTime verifiedAt;

    @ApiModelProperty(value = "管理员ID", example = "100")
    private Long adminId;

    @ApiModelProperty(value = "核销备注", example = "用户已入场")
    private String remarks;

    @ApiModelProperty(value = "订单信息")
    private OrderInfo orderInfo;

    @Data
    @ApiModel("订单信息")
    public static class OrderInfo {

        @ApiModelProperty(value = "订单号", example = "202511291235001234")
        private String orderNo;

        @ApiModelProperty(value = "主题名称", example = "民国年华 - 上海滩之夜")
        private String themeTitle;

        @ApiModelProperty(value = "订单金额（元）", example = "198.00")
        private Double amount;
    }
}
