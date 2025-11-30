package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 退款请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 退款金额（元），不能超过支付金额
     */
    @NotNull(message = "退款金额不能为空")
    private Double refundAmount;

    /**
     * 退款原因
     */
    @NotBlank(message = "退款原因不能为空")
    private String reason;
}
