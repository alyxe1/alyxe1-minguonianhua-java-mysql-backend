package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreatePaymentOrderRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.PaymentNotifyRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.RefundRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreatePaymentOrderResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PaymentNotifyResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PaymentStatusResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.RefundResponse;
import org.exh.nianhuawechatminiprogrambackend.service.PaymentService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 支付控制器
 */
@Api(tags = "微信支付模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @ApiOperation(value = "创建支付订单", notes = "用户点击\"去支付\"时调用此接口，创建订单并返回微信支付的调起参数")
    @PostMapping("/create-order")
    public Result<CreatePaymentOrderResponse> createPaymentOrder(
            @ApiParam(value = "创建支付订单请求", required = true)
            @RequestBody @Valid CreatePaymentOrderRequest request) {

        log.info("创建支付订单请求, bookingId={}, openid={}",
                request.getBookingId(), request.getOpenid());

        CreatePaymentOrderResponse response = paymentService.createPaymentOrder(request);

        log.info("创建支付订单成功, orderNo={}, prepayId={}",
                response.getOrderNo(), response.getPrepayId());

        return Result.success(response);
    }

    @ApiOperation(value = "微信支付结果回调通知", notes = "微信支付成功后，微信服务器异步调用此接口，需要解密并验证签名")
    @PostMapping("/notify")
    public PaymentNotifyResponse paymentNotify(
            @ApiParam(value = "微信支付回调通知", required = true)
            @RequestBody PaymentNotifyRequest notifyRequest) {

        log.info("微信支付回调通知, id={}, eventType={}",
                notifyRequest.getId(), notifyRequest.getEventType());

        PaymentNotifyResponse response = paymentService.handlePaymentNotify(notifyRequest);

        log.info("微信支付回调处理完成, code={}, message={}",
                response.getCode(), response.getMessage());

        return response;
    }

    @ApiOperation(value = "申请退款", notes = "用户申请退款，需要验证订单状态为已支付，退款金额不能超过支付金额，与预订状态联动更新")
    @PostMapping("/refund")
    public RefundResponse applyRefund(
            @ApiParam(value = "退款请求", required = true)
            @RequestBody @Valid RefundRequest request) {

        log.info("申请退款请求, orderNo={}, refundAmount={}, reason={}",
                request.getOrderNo(), request.getRefundAmount(), request.getReason());

        RefundResponse response = paymentService.applyRefund(request);

        log.info("申请退款完成, orderNo={}, code={}, message={}",
                request.getOrderNo(), response.getCode(), response.getMessage());

        return response;
    }

    @ApiOperation(value = "查询支付状态", notes = "前端轮询查询支付状态，使用场景：调起支付后查询结果、用户返回后查询是否成功")
    @GetMapping("/status/{orderNo}")
    public PaymentStatusResponse queryPaymentStatus(
            @ApiParam(value = "订单号（预订时生成的订单号）", required = true, example = "202511291235001234")
            @PathVariable("orderNo") String orderNo) {

        log.info("查询支付状态请求, orderNo={}", orderNo);

        PaymentStatusResponse response = paymentService.queryPaymentStatus(orderNo);

        log.info("查询支付状态完成, orderNo={}, code={}, status={}",
                orderNo, response.getCode(),
                response.getData() != null ? response.getData().getStatus() : "null");

        return response;
    }
}
