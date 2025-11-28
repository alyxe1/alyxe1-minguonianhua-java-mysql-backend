package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建预订响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingResponse {

    /**
     * 预订ID
     */
    @NotBlank(message = "预订ID不能为空")
    private String orderId;

    /**
     * 订单金额（分）
     */
    @NotNull(message = "金额不能为空")
    private Long amount;

    /**
     * 支付状态：pending-待支付, paid-已支付, failed-支付失败
     */
    @NotBlank(message = "支付状态不能为空")
    private String paymentStatus;

    /**
     * 支付过期时间
     */
    private String expireTime;
}
