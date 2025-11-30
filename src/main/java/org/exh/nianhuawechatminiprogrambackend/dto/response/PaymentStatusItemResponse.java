package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付状态详情响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusItemResponse {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 预订ID
     */
    private String bookingId;

    /**
     * 支付状态
     * PENDING: 待支付
     * SUCCESS: 支付成功
     * FAILED: 支付失败
     * CLOSED: 已关闭
     * REFUNDING: 退款中
     * REFUNDED: 已退款
     */
    private String status;

    /**
     * 支付金额（元）
     */
    private Double amount;

    /**
     * 支付时间（ISO 8601格式）
     */
    private String paidTime;

    /**
     * 微信支付交易号
     */
    private String transactionId;
}
