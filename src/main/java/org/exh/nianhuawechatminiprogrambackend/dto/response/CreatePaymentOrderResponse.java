package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建支付订单响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentOrderResponse {

    /**
     * 订单号（复用预订的order_no）
     */
    private String orderNo;

    /**
     * 微信预支付交易会话标识
     */
    private String prepayId;

    /**
     * 小程序AppID
     */
    private String appId;

    /**
     * 时间戳，从1970年1月1日00:00:00至今的秒数
     */
    private String timeStamp;

    /**
     * 随机字符串，不长于32位
     */
    private String nonceStr;

    /**
     * 订单详情扩展字符串，格式：prepay_id=xxx
     */
    private String packageValue;

    /**
     * 签名类型，固定为RSA
     */
    private String signType;

    /**
     * 签名，使用商户API私钥签名
     */
    private String paySign;

    /**
     * 支付金额（元）
     */
    private Double amount;

    /**
     * 订单过期时间（ISO 8601格式）
     */
    private String expireTime;
}
