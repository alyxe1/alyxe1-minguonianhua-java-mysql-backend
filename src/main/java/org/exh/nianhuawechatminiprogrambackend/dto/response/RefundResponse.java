package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 退款数据
     */
    private RefundData data;

    /**
     * 退款数据内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundData {
        /**
         * 退款ID
         */
        private String refundId;

        /**
         * 退款订单号
         */
        private String refundOrderNo;

        /**
         * 退款金额（元）
         */
        private Double refundAmount;

        /**
         * 退款状态 PROCESSING, SUCCESS, FAILED, CLOSED
         */
        private String status;
    }

    /**
     * 构建成功响应
     */
    public static RefundResponse success(String refundId, String refundOrderNo, Double refundAmount, String status) {
        RefundResponse response = new RefundResponse();
        response.setCode(200);
        response.setMessage("退款申请提交成功");

        RefundData data = new RefundData();
        data.setRefundId(refundId);
        data.setRefundOrderNo(refundOrderNo);
        data.setRefundAmount(refundAmount);
        data.setStatus(status);
        response.setData(data);

        return response;
    }

    /**
     * 构建失败响应
     */
    public static RefundResponse error(String message) {
        RefundResponse response = new RefundResponse();
        response.setCode(400);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}
