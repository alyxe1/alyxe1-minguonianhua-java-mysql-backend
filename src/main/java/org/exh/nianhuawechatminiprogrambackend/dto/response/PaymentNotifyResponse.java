package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付回调通知响应 DTO
 * 微信要求返回特定的JSON格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyResponse {

    /**
     * 返回状态码
     * SUCCESS: 成功
     * FAIL: 失败
     */
    private String code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 成功响应
     */
    public static PaymentNotifyResponse success() {
        return new PaymentNotifyResponse("SUCCESS", "OK");
    }

    /**
     * 失败响应
     */
    public static PaymentNotifyResponse fail(String message) {
        return new PaymentNotifyResponse("FAIL", message);
    }
}
