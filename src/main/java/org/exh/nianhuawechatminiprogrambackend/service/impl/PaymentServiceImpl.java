package org.exh.nianhuawechatminiprogrambackend.service.impl;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.config.WxConfig;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreatePaymentOrderRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.PaymentNotifyRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.RefundRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreatePaymentOrderResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PaymentNotifyResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PaymentStatusItemResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PaymentStatusResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.RefundResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.WechatPaymentNotification;
import org.exh.nianhuawechatminiprogrambackend.entity.Booking;
import org.exh.nianhuawechatminiprogrambackend.entity.Order;
import org.exh.nianhuawechatminiprogrambackend.entity.Refund;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.entity.VerificationCode;
import org.exh.nianhuawechatminiprogrambackend.mapper.BookingMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.DailySessionMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.OrderMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.RefundMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SessionMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.VerificationCodeMapper;
import org.exh.nianhuawechatminiprogrambackend.service.PaymentService;
import org.exh.nianhuawechatminiprogrambackend.util.VerificationCodeUtil;
import org.exh.nianhuawechatminiprogrambackend.util.WechatPayDecryptUtil;
import org.exh.nianhuawechatminiprogrambackend.util.WechatPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付服务实现类
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RefundMapper refundMapper;

    @Autowired
    private WxConfig wxConfig;

    @Autowired
    private VerificationCodeUtil verificationCodeUtil;

    @Autowired
    private VerificationCodeMapper verificationCodeMapper;

    @Autowired
    private DailySessionMapper dailySessionMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatePaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest request) {
        log.info("创建支付订单请求: {}", request);

        // 1. 查询预订
        Long bookingId = Long.parseLong(request.getBookingId());
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.error("预订不存在: bookingId={}", bookingId);
            throw new BusinessException(ResultCode.NOT_FOUND, "预订不存在");
        }

        // 2. 验证预订状态（必须是待支付状态）
        if (booking.getStatus() != 0) { // 0-待支付
            log.error("预订状态错误, bookingId={}, status={}", bookingId, booking.getStatus());
            throw new BusinessException(ResultCode.CONFLICT, "预订状态不是待支付");
        }

        // 3. 检查订单是否已存在（幂等性）
        Order existingOrder = orderMapper.selectByOrderNo(booking.getOrderNo());
        if (existingOrder != null) {
            log.info("订单已存在, orderNo={}", booking.getOrderNo());
            // 订单已存在，直接返回支付参数
            return buildPaymentResponse(existingOrder, booking, request.getOpenid());
        }

        // 4. 创建订单（复用预订的order_no）
        Order order = new Order();
        order.setUserId(booking.getUserId());
        order.setBookingId(booking.getId());
        order.setOrderNo(booking.getOrderNo());
        order.setTotalAmount(booking.getTotalAmount());
        order.setPayAmount(booking.getTotalAmount());
        order.setStatus(0); // 0-待支付
        order.setPaymentMethod("wechat"); // 微信支付
        order.setPaymentTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0)); // 初始化为Epoch时间

        orderMapper.insert(order);
        log.info("创建订单成功, orderId={}, orderNo={}", order.getId(), order.getOrderNo());

        // 5. 调用微信统一下单并返回支付参数
        return buildPaymentResponse(order, booking, request.getOpenid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentNotifyResponse handlePaymentNotify(PaymentNotifyRequest notifyRequest) {
        log.info("微信支付回调通知: id={}, eventType={}", notifyRequest.getId(), notifyRequest.getEventType());

        try {
            // 1. 解密数据
            String apiv3Key = wxConfig.getPay().getApiv3Key();
            WechatPaymentNotification payment = WechatPayDecryptUtil.decryptAndParse(
                    apiv3Key,
                    notifyRequest.getResource(),
                    WechatPaymentNotification.class
            );

            log.info("微信支付通知解密成功, outTradeNo={}, transactionId={}, tradeState={}",
                    payment.getOutTradeNo(), payment.getTransactionId(), payment.getTradeState());

            // 2. 验证交易状态-只处理成功状态
            if (!"SUCCESS".equals(payment.getTradeState())) {
                log.warn("支付状态不是成功, tradeState={}", payment.getTradeState());
                return PaymentNotifyResponse.fail("支付状态不是成功: " + payment.getTradeState());
            }

            // 3. 查询订单和预订
            Order order = orderMapper.selectByOrderNo(payment.getOutTradeNo());
            if (order == null) {
                log.error("订单不存在, orderNo={}", payment.getOutTradeNo());
                throw new RuntimeException("订单不存在: " + payment.getOutTradeNo());
            }

            Booking booking = bookingMapper.selectById(order.getBookingId());
            if (booking == null) {
                log.error("预订不存在, bookingId={}", order.getBookingId());
                throw new RuntimeException("预订不存在: " + order.getBookingId());
            }

            // 4. 验证金额是否一致（防止伪造通知）
            if (!order.getPayAmount().equals(payment.getAmount().getTotal())) {
                log.error("支付金额不匹配, 订单金额={}, 支付金额={}",
                        order.getPayAmount(), payment.getAmount().getTotal());
                throw new RuntimeException("支付金额不匹配");
            }

            // 5. 检查订单是否已处理（幂等性）
            if (order.getStatus() == 1) { // 1-已支付
                log.info("订单已处理, orderNo={}", order.getOrderNo());
                return PaymentNotifyResponse.success();
            }

            // 6. 更新订单状态
            order.setStatus(1); // 1-已支付
            order.setTransactionId(payment.getTransactionId());
            // 解析支付时间
            if (payment.getSuccessTime() != null) {
                order.setPaymentTime(LocalDateTime.parse(payment.getSuccessTime().replace("T", " ").substring(0, 19)));
            }
            orderMapper.updateById(order);
            log.info("更新订单状态成功, orderNo={}, status=已支付", order.getOrderNo());

            // 7. 更新预订状态
            booking.setStatus(1); // 1-已支付
            bookingMapper.updateById(booking);
            log.info("更新预订状态成功, bookingId={}, status=已支付", booking.getId());

            // 8. 生成核销码和二维码
            try {
                // 生成核销码和二维码
                VerificationCodeUtil.VerificationCodeResult verificationResult = verificationCodeUtil.generateVerificationCodeWithQrCode();
                log.info("生成核销码成功, code={}, qrCodeUrl={}", verificationResult.getCode(), verificationResult.getQrCodeUrl());

                // 查询场次信息以设置核销码过期时间
                LocalDateTime expiryTime = calculateVerificationCodeExpiryTime(booking);

                // 保存核销码记录
                VerificationCode verificationCode = new VerificationCode();
                verificationCode.setOrderId(order.getId());
                verificationCode.setCode(verificationResult.getCode());
                verificationCode.setQrCodeUrl(verificationResult.getQrCodeUrl());
                verificationCode.setStatus(0); // 0-未使用
                verificationCode.setExpiryTime(expiryTime);
                verificationCodeMapper.insert(verificationCode);
                log.info("保存核销码记录成功, verificationCodeId={}, orderId={}", verificationCode.getId(), order.getId());

            } catch (Exception e) {
                // 核销码生成失败不阻塞支付流程，记录错误日志即可
                log.error("生成或保存核销码失败, orderNo={}, bookingId={}", order.getOrderNo(), booking.getId(), e);
            }

            // 9. 返回成功响应给微信
            log.info("微信支付回调处理成功, orderNo={}", order.getOrderNo());
            return PaymentNotifyResponse.success();

        } catch (Exception e) {
            log.error("微信支付回调处理失败", e);
            return PaymentNotifyResponse.fail("SYSTEM_ERROR: " + e.getMessage());
        }
    }

    @Override
    public PaymentStatusResponse queryPaymentStatus(String orderNo) {
        log.info("查询支付状态请求, orderNo={}", orderNo);

        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("订单不存在, orderNo={}", orderNo);
            return PaymentStatusResponse.error("订单不存在: " + orderNo);
        }

        // 2. 查询预订
        Booking booking = bookingMapper.selectById(order.getBookingId());
        if (booking == null) {
            log.error("预订不存在, bookingId={}", order.getBookingId());
            return PaymentStatusResponse.error("预订不存在: " + order.getBookingId());
        }

        // 3. 根据订单状态映射为统一的支付状态
        String paymentStatus = mapOrderStatusToPaymentStatus(order.getStatus());

        // 4. 计算金额（元）
        Double amountInYuan = order.getPayAmount() / 100.0;

        // 5. 构建支付时间字符串（ISO 8601格式）
        String paidTime = null;
        if (order.getPaymentTime() != null && !order.getPaymentTime().equals(LocalDateTime.of(1970, 1, 1, 0, 0, 0))) {
            paidTime = order.getPaymentTime().toString().replace(" ", "T") + "+08:00";
        }

        // 6. 构建响应
        PaymentStatusItemResponse itemResponse = new PaymentStatusItemResponse();
        itemResponse.setOrderNo(order.getOrderNo());
        itemResponse.setBookingId(String.valueOf(booking.getId()));
        itemResponse.setStatus(paymentStatus);
        itemResponse.setAmount(amountInYuan);
        itemResponse.setPaidTime(paidTime);
        itemResponse.setTransactionId(order.getTransactionId());

        log.info("查询支付状态成功, orderNo={}, status={}", orderNo, paymentStatus);
        return PaymentStatusResponse.success(itemResponse);
    }

    /**
     * 将订单状态映射为统一的支付状态
     * @param orderStatus 订单状态 (0-待支付, 1-已支付, 2-支付失败, 3-已退款)
     * @return 支付状态字符串
     */
    private String mapOrderStatusToPaymentStatus(Integer orderStatus) {
        switch (orderStatus) {
            case 0:
                return "PENDING";      // 待支付
            case 1:
                return "SUCCESS";      // 支付成功
            case 2:
                return "FAILED";       // 支付失败
            case 3:
                return "REFUNDED";     // 已退款
            default:
                return "UNKNOWN";      // 未知状态
        }
    }

    /**
     * 构建支付响应参数
     * @param order 订单
     * @param booking 预订
     * @param openid 用户openid
     * @return 支付响应
     */
    private CreatePaymentOrderResponse buildPaymentResponse(Order order, Booking booking, String openid) {
        // 调用微信统一下单接口（暂时使用模拟实现）
        String prepayId = WechatPayUtil.unifiedOrder(
                wxConfig.getMiniapp().getAppid(),
                wxConfig.getPay().getMchid(),
                wxConfig.getPay().getSerialNumber(),
                "", // TODO: 私钥路径，暂时留空使用模拟
                wxConfig.getPay().getApiv3Key(),
                "民国年华预订",
                order.getOrderNo(),
                order.getPayAmount(),
                openid,
                wxConfig.getPay().getNotifyUrl()
        );

        // 计算金额（元）
        Double amountInYuan = order.getPayAmount() / 100.0;

        // 生成支付参数
        String privateKey = ""; // TODO: 实际项目中需要读取商户私钥
        Map<String, Object> paymentParams = WechatPayUtil.buildPaymentParams(
                wxConfig.getMiniapp().getAppid(),
                prepayId,
                privateKey,
                amountInYuan
        );

        // 构建响应
        CreatePaymentOrderResponse response = new CreatePaymentOrderResponse();
        response.setOrderNo(order.getOrderNo());
        response.setPrepayId((String) paymentParams.get("prepayId"));
        response.setAppId((String) paymentParams.get("appId"));
        response.setTimeStamp((String) paymentParams.get("timeStamp"));
        response.setNonceStr((String) paymentParams.get("nonceStr"));
        response.setPackageValue((String) paymentParams.get("packageValue"));
        response.setSignType((String) paymentParams.get("signType"));
        response.setPaySign((String) paymentParams.get("paySign"));
        response.setAmount(amountInYuan);
        response.setExpireTime(booking.getCreatedAt().plusMinutes(10).toString()); // 预订后10分钟过期

        log.info("创建支付订单成功, orderNo={}, prepayId={}", order.getOrderNo(), response.getPrepayId());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResponse applyRefund(RefundRequest request) {
        log.info("申请退款请求: orderNo={}, refundAmount={}, reason={}",
                request.getOrderNo(), request.getRefundAmount(), request.getReason());

        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(request.getOrderNo());
        if (order == null) {
            log.error("订单不存在, orderNo={}", request.getOrderNo());
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 2. 验证订单状态（必须是已支付状态）
        if (order.getStatus() != 1) { // 1-已支付
            log.error("订单状态不是已支付, orderNo={}, status={}", request.getOrderNo(), order.getStatus());
            throw new BusinessException(ResultCode.CONFLICT, "订单状态不是已支付，不能退款");
        }

        // 3. 验证退款金额不能超过支付金额
        Long refundAmountInFen = (long) (request.getRefundAmount() * 100);
        if (refundAmountInFen > order.getPayAmount()) {
            log.error("退款金额超过支付金额, orderNo={}, refundAmount={}, payAmount={}",
                    request.getOrderNo(), refundAmountInFen, order.getPayAmount());
            throw new BusinessException(ResultCode.BAD_REQUEST, "退款金额不能超过支付金额");
        }

        // 4. 检查是否已经存在退款记录（避免重复退款）
        Refund existingRefund = refundMapper.selectByOrderNo(request.getOrderNo());
        if (existingRefund != null && existingRefund.getStatus() != 2) { // 2-退款失败，允许重新申请
            log.warn("订单已申请过退款, orderNo={}, refundStatus={}",
                    request.getOrderNo(), existingRefund.getStatus());
            if (existingRefund.getStatus() == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "退款申请正在处理中，请勿重复申请");
            } else if (existingRefund.getStatus() == 1) {
                throw new BusinessException(ResultCode.CONFLICT, "退款已成功，不能重复申请");
            }
        }

        // 5. 查询预订并验证状态
        Booking booking = bookingMapper.selectById(order.getBookingId());
        if (booking == null) {
            log.error("预订不存在, bookingId={}", order.getBookingId());
            throw new BusinessException(ResultCode.NOT_FOUND, "预订不存在");
        }

        if (booking.getStatus() != 1) { // 1-已支付
            log.error("预订状态不是已支付, bookingId={}, status={}", booking.getId(), booking.getStatus());
            throw new BusinessException(ResultCode.CONFLICT, "预订状态不是已支付，不能退款");
        }

        // 6. 生成退款单号
        String refundNo = generateRefundNo();
        log.info("生成退款单号: {}", refundNo);

        // 7. 创建退款记录
        Refund refund = new Refund();
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setRefundNo(refundNo);
        refund.setTransactionId(order.getTransactionId());
        refund.setTotalAmount(order.getPayAmount());
        refund.setRefundAmount(refundAmountInFen);
        refund.setStatus(0); // 0-退款中
        refund.setReason(request.getReason());
        refundMapper.insert(refund);
        log.info("创建退款记录成功, refundId={}, refundNo={}", refund.getId(), refundNo);

        // 8. 调用微信退款接口（模拟实现）
        // TODO: 实际项目中需要调用微信退款API
        String wechatRefundId = "WXREFUND" + IdUtil.fastSimpleUUID();
        log.info("模拟微信退款成功, wechatRefundId={}", wechatRefundId);

        // 9. 更新退款记录为成功状态
        refund.setStatus(1); // 1-退款成功
        refund.setRefundId(wechatRefundId);
        refundMapper.updateById(refund);
        log.info("更新退款状态为成功, refundNo={}", refundNo);

        // 10. 更新订单状态为已退款
        order.setStatus(3); // 3-已退款
        orderMapper.updateById(order);
        log.info("更新订单状态为已退款, orderNo={}", order.getOrderNo());

        // 11. 更新预订状态为已退款（与预订联动）
        booking.setStatus(3); // 3-已退款（扩展状态：0-待支付, 1-已支付, 2-已取消, 3-已退款）
        bookingMapper.updateById(booking);
        log.info("更新预订状态为已退款, bookingId={}", booking.getId());

        // 12. 返回退款响应
        Double refundAmountInYuan = refundAmountInFen / 100.0;
        return RefundResponse.success(
                String.valueOf(refund.getId()),
                refundNo,
                refundAmountInYuan,
                "SUCCESS"
        );
    }

    /**
     * 生成退款单号
     * @return 退款单号
     */
    private String generateRefundNo() {
        // 退款单号格式：R + 年月日时分 + 随机6位
        return "R" + IdUtil.fastSimpleUUID();
    }

    /**
     * 计算核销码过期时间
     * 核销码在场次结束后24小时过期
     * @param booking 预订信息
     * @return 核销码过期时间
     */
    private LocalDateTime calculateVerificationCodeExpiryTime(Booking booking) {
        try {
            // 查询每日场次信息
            org.exh.nianhuawechatminiprogrambackend.entity.DailySession dailySession =
                    dailySessionMapper.selectById(booking.getDailySessionId());
            if (dailySession == null) {
                log.warn("未找到每日场次信息, dailySessionId={}, 使用默认过期时间(预订日期+1天)", booking.getDailySessionId());
                return booking.getBookingDate().atTime(23, 59, 59).plusDays(1);
            }

            // 查询场次模板信息
            org.exh.nianhuawechatminiprogrambackend.entity.Session session =
                    sessionMapper.selectById(dailySession.getSessionId());
            if (session == null) {
                log.warn("未找到场次模板信息, sessionId={}, 使用默认过期时间(预订日期+1天)", dailySession.getSessionId());
                return booking.getBookingDate().atTime(23, 59, 59).plusDays(1);
            }

            // 核销码过期时间 = 场次结束时间 + 24小时
            LocalDateTime sessionEndTime = booking.getBookingDate().atTime(session.getEndTime());
            LocalDateTime expiryTime = sessionEndTime.plusHours(24);

            log.info("计算核销码过期时间成功, bookingDate={}, sessionEndTime={}, expiryTime={}",
                    booking.getBookingDate(), sessionEndTime, expiryTime);

            return expiryTime;

        } catch (Exception e) {
            log.error("计算核销码过期时间失败, 使用默认过期时间", e);
            // 默认过期时间：预订日期当天晚上23:59:59 + 1天
            return booking.getBookingDate().atTime(23, 59, 59).plusDays(1);
        }
    }
}
