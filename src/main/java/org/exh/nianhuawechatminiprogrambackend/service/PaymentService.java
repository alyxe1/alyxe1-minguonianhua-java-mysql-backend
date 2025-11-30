package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.request.CreatePaymentOrderRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.PaymentNotifyRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.RefundRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreatePaymentOrderResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PaymentNotifyResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PaymentStatusResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.RefundResponse;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付订单
     * @param request 创建支付订单请求
     * @return 创建支付订单响应
     */
    CreatePaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest request);

    /**
     * 处理微信支付结果回调通知
     * @param notifyRequest 支付通知请求
     * @return 支付通知响应
     */
    PaymentNotifyResponse handlePaymentNotify(PaymentNotifyRequest notifyRequest);

    /**
     * 查询支付状态
     * @param orderNo 订单号
     * @return 支付状态响应
     */
    PaymentStatusResponse queryPaymentStatus(String orderNo);

    /**
     * 申请退款
     * @param request 退款请求
     * @return 退款响应
     */
    RefundResponse applyRefund(RefundRequest request);
}
