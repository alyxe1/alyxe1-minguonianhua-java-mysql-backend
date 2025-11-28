package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建预订响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingResponse {

    /**
     * 订单ID（预订ID）
     */
    private String orderId;

    /**
     * 订单总金额（分）
     */
    private Integer amount;

    /**
     * 支付状态
     */
    private String paymentStatus;

    /**
     * 过期时间
     */
    private String expireTime;
}
