package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询支付状态响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 支付状态详情数据
     */
    private PaymentStatusItemResponse data;

    /**
     * 构建成功响应
     */
    public static PaymentStatusResponse success(PaymentStatusItemResponse data) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setCode(200);
        response.setMessage("查询成功");
        response.setData(data);
        return response;
    }

    /**
     * 构建失败响应
     */
    public static PaymentStatusResponse error(String message) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setCode(500);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}
