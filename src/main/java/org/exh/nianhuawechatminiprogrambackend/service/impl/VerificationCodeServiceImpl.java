package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.OrderVerificationInfo;
import org.exh.nianhuawechatminiprogrambackend.dto.response.VerificationConfirmResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.VerificationVerifyResult;
import org.exh.nianhuawechatminiprogrambackend.entity.Booking;
import org.exh.nianhuawechatminiprogrambackend.entity.DailySession;
import org.exh.nianhuawechatminiprogrambackend.entity.Order;
import org.exh.nianhuawechatminiprogrambackend.entity.Session;
import org.exh.nianhuawechatminiprogrambackend.entity.Theme;
import org.exh.nianhuawechatminiprogrambackend.entity.User;
import org.exh.nianhuawechatminiprogrambackend.entity.VerificationCode;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.BookingMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.DailySessionMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.OrderMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SessionMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.ThemeMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.UserMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.VerificationCodeMapper;
import org.exh.nianhuawechatminiprogrambackend.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 核销码服务实现类
 */
@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private VerificationCodeMapper verificationCodeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private ThemeMapper themeMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private DailySessionMapper dailySessionMapper;

    @Override
    public VerificationCode getVerificationCodeByOrderNo(String orderNo) {
        log.info("获取核销码请求, orderNo={}", orderNo);

        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("订单不存在, orderNo={}", orderNo);
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 2. 根据订单ID查询核销码
        VerificationCode verificationCode = verificationCodeMapper.selectByOrderId(order.getId());
        if (verificationCode == null) {
            log.error("核销码不存在, orderNo={}, orderId={}", orderNo, order.getId());
            throw new BusinessException(ResultCode.NOT_FOUND, "核销码不存在");
        }

        log.info("获取核销码成功, orderNo={}, code={}, qrCodeUrl={}",
                orderNo, verificationCode.getCode(), verificationCode.getQrCodeUrl());

        return verificationCode;
    }

    @Override
    public VerificationVerifyResult verify(String code) {
        log.info("核销验证请求, code={}", code);

        // 1. 根据核销码查询核销记录
        VerificationCode verificationCode = verificationCodeMapper.selectByCode(code);
        if (verificationCode == null) {
            log.error("核销码不存在, code={}", code);
            throw new BusinessException(ResultCode.NOT_FOUND, "核销码不存在");
        }

        // 2. 查询订单信息
        Order order = orderMapper.selectById(verificationCode.getOrderId());
        if (order == null) {
            log.error("订单不存在, orderId={}", verificationCode.getOrderId());
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 3. 查询预订信息
        Booking booking = bookingMapper.selectById(order.getBookingId());
        if (booking == null) {
            log.error("预订不存在, bookingId={}", order.getBookingId());
            throw new BusinessException(ResultCode.NOT_FOUND, "预订不存在");
        }

        // 4. 查询用户信息（联系人）
        User user = userMapper.selectById(order.getUserId());
        if (user == null) {
            log.error("用户不存在, userId={}", order.getUserId());
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 5. 查询主题信息
        Theme theme = themeMapper.selectById(booking.getThemeId());
        if (theme == null) {
            log.error("主题不存在, themeId={}", booking.getThemeId());
            throw new BusinessException(ResultCode.NOT_FOUND, "主题不存在");
        }

        // 6. 查询每日场次信息
        DailySession dailySession = dailySessionMapper.selectById(booking.getDailySessionId());
        if (dailySession == null) {
            log.error("每日场次不存在, dailySessionId={}", booking.getDailySessionId());
            throw new BusinessException(ResultCode.NOT_FOUND, "每日场次不存在");
        }

        // 7. 查询场次模板信息
        Session session = sessionMapper.selectById(dailySession.getSessionId());
        if (session == null) {
            log.error("场次模板不存在, sessionId={}", dailySession.getSessionId());
            throw new BusinessException(ResultCode.NOT_FOUND, "场次模板不存在");
        }

        // 8. 构建响应对象
        OrderVerificationInfo orderInfo = new OrderVerificationInfo();
        orderInfo.setOrderNo(order.getOrderNo());
        orderInfo.setThemeTitle(theme.getTitle());
        orderInfo.setAmount(order.getPayAmount() / 100.0); // 转换为元
        orderInfo.setPeopleCount(booking.getSeatCount());
        orderInfo.setContactName(user.getNickname());
        orderInfo.setContactPhone(user.getPhone());

        // 构建场次时间：预订日期 + 场次开始时间
        LocalDateTime sessionTime = booking.getBookingDate().atTime(session.getStartTime());
        orderInfo.setSessionTime(sessionTime);

        // 构建最终结果
        VerificationVerifyResult result = new VerificationVerifyResult();
        result.setOrderInfo(orderInfo);

        log.info("核销验证成功, code={}, orderNo={}, theme={}, people={}, sessionTime={}",
                code, order.getOrderNo(), theme.getTitle(), booking.getSeatCount(), sessionTime);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VerificationConfirmResponse confirm(String code, String remarks, Long adminId) {
        log.info("核销确认请求, code={}, remarks={}, adminId={}", code, remarks, adminId);

        // 1. 根据核销码查询核销记录
        VerificationCode verificationCode = verificationCodeMapper.selectByCode(code);
        if (verificationCode == null) {
            log.error("核销码不存在, code={}", code);
            throw new BusinessException(ResultCode.NOT_FOUND, "核销码不存在");
        }

        // 2. 检查核销码状态
        if (verificationCode.getStatus() == 1) {
            log.error("核销码已使用, code={}", code);
            throw new BusinessException(ResultCode.CONFLICT, "核销码已使用");
        } else if (verificationCode.getStatus() == 2) {
            log.error("核销码已过期, code={}", code);
            throw new BusinessException(ResultCode.CONFLICT, "核销码已过期");
        }

        // 3. 查询订单信息（用于返回响应）
        Order order = orderMapper.selectById(verificationCode.getOrderId());
        if (order == null) {
            log.error("订单不存在, orderId={}", verificationCode.getOrderId());
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 4. 查询主题信息（用于返回响应）
        Booking booking = bookingMapper.selectById(order.getBookingId());
        if (booking == null) {
            log.error("预订不存在, bookingId={}", order.getBookingId());
            throw new BusinessException(ResultCode.NOT_FOUND, "预订不存在");
        }

        Theme theme = themeMapper.selectById(booking.getThemeId());
        if (theme == null) {
            log.error("主题不存在, themeId={}", booking.getThemeId());
            throw new BusinessException(ResultCode.NOT_FOUND, "主题不存在");
        }

        // 5. 更新核销码状态（实际核销操作）
        LocalDateTime now = LocalDateTime.now();
        verificationCode.setStatus(1); // 1-已使用
        verificationCode.setVerifiedAt(now);
        verificationCode.setAdminId(adminId);
        verificationCode.setRemarks(remarks);
        verificationCode.setUpdatedAt(now);

        int rows = verificationCodeMapper.updateById(verificationCode);
        if (rows != 1) {
            log.error("更新核销码状态失败, verificationCodeId={}", verificationCode.getId());
            throw new BusinessException(ResultCode.ERROR, "核销失败，请重试");
        }

        // 6. 构建响应
        VerificationConfirmResponse response = new VerificationConfirmResponse();
        response.setVerificationId(verificationCode.getId());
        response.setCode(verificationCode.getCode());
        response.setVerifiedAt(verificationCode.getVerifiedAt());
        response.setAdminId(adminId);
        response.setRemarks(remarks);

        // 订单信息
        VerificationConfirmResponse.OrderInfo orderInfo = new VerificationConfirmResponse.OrderInfo();
        orderInfo.setOrderNo(order.getOrderNo());
        orderInfo.setThemeTitle(theme.getTitle());
        orderInfo.setAmount(order.getPayAmount() / 100.0); // 转换为元
        response.setOrderInfo(orderInfo);

        log.info("核销确认成功, code={}, verificationId={}, orderNo={}",
                code, verificationCode.getId(), order.getOrderNo());

        return response;
    }
}
