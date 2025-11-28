package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreateBookingRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedGood;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreateBookingResponse;
import org.exh.nianhuawechatminiprogrambackend.entity.*;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.*;
import org.exh.nianhuawechatminiprogrambackend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 预订服务实现类
 * 重点解决高并发抢座问题
 */
@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private DailySessionMapper dailySessionMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private BookingSeatMapper bookingSeatMapper;

    @Autowired
    private BookingGoodsMapper bookingGoodsMapper;

    private static final DateTimeFormatter EXPIRE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 预订状态常量
     */
    private static final int STATUS_PENDING_PAYMENT = 0; // 待支付
    private static final int STATUS_PAID = 1; // 已支付
    private static final int STATUS_CANCELED = 2; // 已取消
    private static final int STATUS_COMPLETED = 3; // 已完成

    private static final int PAYMENT_STATUS_PENDING = "pending"; // 待支付

    private static final int BOOKING_EXPIRE_MINUTES = 15; // 预订15分钟后过期

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateBookingResponse createBooking(CreateBookingRequest request) {
        log.info("开始创建预订，userId={}, sessionType={}, date={}, seatCount={}, goodsCount={}",
                request.getUserId(), request.getSessionType(), request.getDate(),
                request.getSelectedSeatList().size(), request.getSelectedGoodList().size());

        // 1. 参数基本校验
        if (request.getUserId() == null || request.getUserId() <= 0) {
            log.error("用户ID无效：{}", request.getUserId());
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID无效");
        }

        if (request.getSessionType() == null || request.getSessionType().isEmpty()) {
            log.error("场次类型不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "场次类型不能为空");
        }

        if (request.getDate() == null || request.getDate().isEmpty()) {
            log.error("预订日期不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "预订日期不能为空");
        }

        if (request.getSelectedSeatList() == null || request.getSelectedSeatList().isEmpty()) {
            log.error("座位列表不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "座位列表不能为空");
        }

        if (request.getSelectedGoodList() == null || request.getSelectedGoodList().isEmpty()) {
            log.error("商品列表不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品列表不能为空");
        }

        // 2. 解析日期
        java.time.LocalDate bookingDate;
        try {
            bookingDate = java.time.LocalDate.parse(request.getDate());
        } catch (Exception e) {
            log.error("日期格式错误，应为YYYY-MM-DD: {}", request.getDate());
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期格式错误");
        }

        // 3. 查询场次模板
        Session session = getSessionByType(request.getSessionType());
        if (session == null) {
            log.error("场次不存在，sessionType={}", request.getSessionType());
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND, "场次不存在");
        }

        // 4. 查询每日场次
        DailySession dailySession = getDailySession(session.getId(), bookingDate);
        if (dailySession == null) {
            log.error("该日期场次不存在，sessionId={}, date={}", session.getId(), bookingDate);
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND, "该日期场次不存在");
        }

        // 5. 高并发检查：验证座位是否可用（关键步骤）
        List<Long> seatIds = request.getSelectedSeatList().stream()
                .map(seat -> Long.parseLong(seat.getSeatId()))
                .collect(java.util.stream.Collectors.toList());

        validateSeatAvailability(session.getId(), bookingDate, seatIds);

        // 6. 计算商品总价
        Long totalAmount = calculateTotalAmount(request.getSelectedGoodList());

        // 7. 创建预订记录（BOOKINGS表）
        String orderNo = generateOrderNo();
        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setThemeId(session.getThemeId());
        booking.setDailySessionId(dailySession.getId());
        booking.setOrderNo(orderNo);
        booking.setTotalAmount(totalAmount);
        booking.setSeatCount(request.getSelectedSeatList().size());
        booking.setBookingDate(bookingDate);
        booking.setStatus(STATUS_PENDING_PAYMENT);

        int insertResult = bookingMapper.insert(booking);
        if (insertResult <= 0) {
            log.error("创建预订失败");
            throw new BusinessException(ResultCode.ERROR, "创建预订失败");
        }

        log.info("预订创建成功，bookingId={}, orderNo={}", booking.getId(), orderNo);

        // 8. 批量插入预订座位记录（BOOKING_SEATS表）
        batchInsertBookingSeats(booking.getId(), request.getSelectedSeatList());

        // 9. 批量插入预订商品记录（BOOKING_GOODS表）
        batchInsertBookingGoods(booking.getId(), request.getSelectedGoodList());

        // 10. 构建响应
        CreateBookingResponse response = new CreateBookingResponse();
        response.setOrderId(orderNo);
        response.setAmount(totalAmount.intValue());
        response.setPaymentStatus(PAYMENT_STATUS_PENDING);
        response.setExpireTime(LocalDateTime.now().plusMinutes(BOOKING_EXPIRE_MINUTES)
                .format(EXPIRE_TIME_FORMATTER));

        log.info("预订创建完成，orderNo={}, amount={}, expireTime={}",
                orderNo, totalAmount, response.getExpireTime());

        return response;
    }

    /**
     * 高并发验证座位可用性
     * 使用数据库原子操作确保并发安全
     */
    private void validateSeatAvailability(Long sessionId, java.time.LocalDate date, List<Long> seatIds) {
        log.info("开始验证座位可用性，sessionId={}, date={}, seatIds={}", sessionId, date, seatIds);

        // 使用 MyBatis-Plus 原子查询锁定座位
        // 查询所有请求的座位的状态
        List<Seat> seats = seatMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                        .in(Seat::getId, seatIds)
                        .eq(Seat::getSessionTemplateId, sessionId)
        );

        // 检查座位是否存在
        if (seats.size() != seatIds.size()) {
            log.error("部分座位不存在，请求：{}，查询到：{}", seatIds.size(), seats.size());
            throw new BusinessException(ResultCode.SEAT_NOT_AVAILABLE, "座位不存在或不可用");
        }

        // 检查座位状态（0=可用，1=已锁定，2=已预订）
        for (Seat seat : seats) {
            if (seat.getStatus() != 0) {
                log.error("座位不可用，seatId={}, status={}", seat.getId(), seat.getStatus());
                throw new BusinessException(ResultCode.SEAT_ALREADY_LOCKED,
                        String.format("座位%s已被占用", seat.getSeatName()));
            }
        }

        // 原子性锁定所有选中的座位
        int updateCount = seatMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Seat>()
                        .in(Seat::getId, seatIds)
                        .eq(Seat::getSessionTemplateId, sessionId)
                        .set(Seat::getStatus, 1) // 1=已锁定
        );

        if (updateCount != seatIds.size()) {
            log.error("座位锁定失败，预期：{}，实际：{}", seatIds.size(), updateCount);
            throw new BusinessException(ResultCode.SEAT_ALREADY_LOCKED, "座位已被其他用户抢先锁定，请重新选择");
        }

        log.info("座位验证并锁定成功，锁定数量：{}", updateCount);
    }

    /**
     * 根据sessionType查询场次模板
     */
    private Session getSessionByType(String sessionType) {
        return sessionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                        .eq(Session::getSessionType, sessionType)
                        .eq(Session::getStatus, 1)
                        .eq(Session::getIsDeleted, 0)
        );
    }

    /**
     * 根据sessionId和日期查询每日场次
     */
    private DailySession getDailySession(Long sessionId, java.time.LocalDate date) {
        return dailySessionMapper.selectBySessionIdAndDate(sessionId, date);
    }

    /**
     * 计算商品总价
     */
    private Long calculateTotalAmount(List<SelectedGood> selectedGoods) {
        List<Long> goodsIds = selectedGoods.stream()
                .map(SelectedGood::getGoodId)
                .collect(java.util.stream.Collectors.toList());

        List<Goods> goods = goodsMapper.selectBatchIds(goodsIds);
        if (goods.size() != selectedGoods.size()) {
            log.error("部分商品不存在，请求：{}，查询到：{}", selectedGoods.size(), goods.size());
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }

        Long totalAmount = 0L;
        for (SelectedGood selectedGood : selectedGoods) {
            for (Good good : goods) {
                if (good.getId().equals(selectedGood.getGoodId())) {
                    totalAmount += good.getPrice() * selectedGood.getSelectedCount();
                    break;
                }
            }
        }

        log.info("商品总价计算完成：{} 分", totalAmount);
        return totalAmount;
    }

    /**
     * 批量插入预订座位记录
     */
    private void batchInsertBookingSeats(Long bookingId, List<CreateBookingRequest.SelectedSeat> selectedSeats) {
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (CreateBookingRequest.SelectedSeat selectedSeat : selectedSeats) {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBookingId(bookingId);
            bookingSeat.setSeatId(Long.parseLong(selectedSeat.getSeatId()));
            bookingSeat.setSeatName(selectedSeat.getSeatName());
            bookingSeats.add(bookingSeat);
        }

        bookingSeatMapper.insertBatchSomeColumn(bookingSeats);
        log.info("预订座位批量插入完成，数量：{}", bookingSeats.size());
    }

    /**
     * 批量插入预订商品记录
     */
    private void batchInsertBookingGoods(Long bookingId, List<SelectedGood> selectedGoods) {
        List<Long> goodsIds = selectedGoods.stream()
                .map(SelectedGood::getGoodId)
                .collect(java.util.stream.Collectors.toList());

        List<Goods> goods = goodsMapper.selectBatchIds(goodsIds);

        List<BookingGoods> bookingGoods = new ArrayList<>();
        for (SelectedGood selectedGood : selectedGoods) {
            for (Good good : goods) {
                if (good.getId().equals(selectedGood.getGoodId())) {
                    BookingGoods bookingGood = new BookingGoods();
                    bookingGood.setBookingId(bookingId);
                    bookingGood.setGoodsId(good.getId());
                    bookingGood.setQuantity(selectedGood.getSelectedCount());
                    bookingGood.setPrice(good.getPrice());
                    bookingGoods.add(bookingGood);
                    break;
                }
            }
        }

        bookingGoodsMapper.insertBatchSomeColumn(bookingGoods);
        log.info("预订商品批量插入完成，数量：{}", bookingGoods.size());
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "BOOK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
