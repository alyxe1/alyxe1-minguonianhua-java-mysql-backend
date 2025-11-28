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
    private String bookingId;

    /**
     * 总金额（分）
     */
    @NotNull(message = "金额不能为空")
    private Long amount;

    /**
     * 支付状态
     */
    @NotBlank(message = "支付状态不能为空")
    private String paymentStatus;

    /**
     * 过期时间（ISO格式）
     */
    @NotBlank(message = "过期时间不能为空")
    private String expireTime;
}
