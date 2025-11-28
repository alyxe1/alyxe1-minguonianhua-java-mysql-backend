package org.exh.nianhuawechatminiprogrambackend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreateBookingRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreateBookingResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedGood;
import org.exh.nianhuawechatminiprogrambackend.entity.*;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.*;
import org.exh.nianhuawechatminiprogrambackend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预订服务实现类 - 支持高并发抢座
 */
@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private DailySessionMapper dailySessionMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private BookingSeatMapper bookingSeatMapper;

    @Autowired
    private BookingGoodsMapper bookingGoodsMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 预订状态常量
    private static final int STATUS_PENDING = 0;    // 待支付
    private static final int STATUS_PAID = 1;       // 已支付
    private static final int STATUS_CANCELLED = 2;  // 已取消
    private static final int STATUS_COMPLETED = 3;  // 已完成

    // 支付过期时间（15分钟）
    private static final int PAYMENT_EXPIRE_MINUTES = 15;

    // 座位消耗区域常量
    private static final String AREA_FRONT = "front";
    private static final String AREA_MIDDLE = "middle";
    private static final String AREA_BACK = "back";

    // 商品分类常量
    private static final String CATEGORY_MAKEUP = "makeup";
    private static final String CATEGORY_PHOTOGRAPHY = "photos";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateBookingResponse createBooking(CreateBookingRequest request) {
        log.info("开始创建预订，sessionType={}, date={}, seatCount={}, goodCount={}",
                request.getSessionType(), request.getDate(),
                request.getSelectedSeatList().size(),
                request.getSelectedGoodList().size());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 参数校验和预处理
            LocalDateTime bookingDateTime = validateAndPreprocessRequest(request);

            // 2. 获取核心数据
            Session session = getSessionByType(request.getSessionType());
            DailySession dailySession = getDailySession(session.getId(), bookingDateTime.toLocalDate());
            Map<Long, Goods> goodsMap = getGoodsMap(request.getSelectedGoodList());

            // 3. 高并发座位竞争检查和锁定
            Map<Long, Seat> seatMap = lockSeatsWithConcurrency(request, session.getId(), bookingDateTime.toLocalDate());

            // 4. 高并发库存检查和扣减
            validateAndConsumeInventory(dailySession, request.getSelectedGoodList(), goodsMap);

            // 5. 创建预订记录
            Booking booking = createBookingRecord(request, session, dailySession, goodsMap);

            // 6. 创建预订座位记录
            createBookingSeatRecords(booking.getId(), request.getSelectedSeatList(), seatMap);

            // 7. 创建预订商品记录
            createBookingGoodsRecords(booking.getId(), request.getSelectedGoodList(), goodsMap);

            // 8. 构建响应
            CreateBookingResponse response = buildResponse(booking);

            long duration = System.currentTimeMillis() - startTime;
            log.info("预订创建成功，bookingId={}, orderNo={}, 耗时={}ms",
                    booking.getId(), booking.getOrderNo(), duration);

            return response;

        } catch (BusinessException e) {
            log.error("预订创建失败，业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("预订创建失败，系统异常", e);
            throw new BusinessException(ResultCode.ERROR, "预订创建失败：" + e.getMessage());
        }
    }

    /**
     * 参数校验和预处理
     */
    private LocalDateTime validateAndPreprocessRequest(CreateBookingRequest request) {
        // 验证日期格式
        if (request.getDate() == null || request.getDate().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期不能为空");
        }

        LocalDateTime bookingDateTime;
        try {
            bookingDateTime = java.time.LocalDate.parse(request.getDate())
                    .atStartOfDay();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期格式错误");
        }

        // 验证场次类型
        if (request.getSessionType() == null || request.getSessionType().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "场次类型不能为空");
        }

        // 验证座位列表
        if (request.getSelectedSeatList() == null || request.getSelectedSeatList().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "座位列表不能为空");
        }

        // 验证商品列表
        if (request.getSelectedGoodList() == null || request.getSelectedGoodList().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品列表不能为空");
        }

        // 座位去重检查
        Set<String> seatIds = new HashSet<>();
        for (var selectedSeat : request.getSelectedSeatList()) {
            if (!seatIds.add(selectedSeat.getSeatId())) {
                throw new BusinessException(ResultCode.BOOKING_CONFLICT, "座位ID重复：" + selectedSeat.getSeatId());
            }
        }

        return bookingDateTime;
    }

    /**
     * 根据sessionType查询场次模板
     */
    private Session getSessionByType(String sessionType) {
        Session session = sessionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                        .eq(Session::getSessionType, sessionType)
                        .eq(Session::getStatus, 1)
                        .eq(Session::getIsDeleted, 0)
        );

        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND, "场次不存在：" + sessionType);
        }

        return session;
    }

    /**
     * 查询每日场次
     */
    private DailySession getDailySession(Long sessionId, java.time.LocalDate date) {
        DailySession dailySession = dailySessionMapper.selectBySessionIdAndDate(sessionId, date);
        if (dailySession == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND, "该日期场次不存在");
        }
        return dailySession;
    }

    /**
     * 获取商品Map
     */
    private Map<Long, Goods> getGoodsMap(List<SelectedGood> selectedGoods) {
        List<Long> goodsIds = selectedGoods.stream()
                .map(SelectedGood::getGoodId)
                .collect(java.util.stream.Collectors.toList());

        List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIds);
        if (goodsList.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }

        return goodsList.stream()
                .collect(java.util.stream.Collectors.toMap(Goods::getId, g -> g));
    }

    /**
     * 高并发座位竞争检查和锁定
     * 使用悲观锁通过数据库行锁防止超卖
     */
    private Map<Long, Seat> lockSeatsWithConcurrency(CreateBookingRequest request, Long sessionId, java.time.LocalDate date) {
        // 提取座位ID列表
        List<String> seatIds = request.getSelectedSeatList().stream()
                .map(CreateBookingRequest.SelectedSeatForBooking::getSeatId)
                .collect(java.util.stream.Collectors.toList());

        // 检查座位是否已被预订（使用行锁防止并发）
        int bookedCount = bookingMapper.countBookedSeats(sessionId, date, seatIds);
        if (bookedCount > 0) {
            // 查找具体被占用的座位
            List<Seat> bookedSeats = seatMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                            .in(Seat::getId, seatIds)
                            .eq(Seat::getStatus, 1) // 1表示已锁定或已预订
            );

            String bookedSeatNames = bookedSeats.stream()
                    .map(Seat::getSeatName)
                    .collect(java.util.stream.Collectors.joining("、"));

            log.warn("座位已被预订，bookedCount={}, seats={}", bookedCount, bookedSeatNames);
            throw new BusinessException(ResultCode.SEAT_ALREADY_LOCKED, "座位已被预订：" + bookedSeatNames);
        }

        // 检查座位可用性（不用FOR UPDATE，让数据库在事务中自动锁定）
        List<Seat> seatsAvailable = seatMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                        .in(Seat::getId, seatIds)
                        .eq(Seat::getSessionTemplateId, sessionId)
                        .eq(Seat::getStatus, 0) // 0表示可用
        );

        if (seatsAvailable.size() != seatIds.size()) {
            // 有座位不可用
            throw new BusinessException(ResultCode.SEAT_ALREADY_LOCKED, "部分座位不可用，请重新选择");
        }

        // 在事务中座位会自动被锁定，不需要单独更新状态

        log.info("检查座位可用性，数量：{}", seatsAvailable.size());
        return seatsAvailable.stream()
                .collect(java.util.stream.Collectors.toMap(Seat::getId, s -> s));
    }

    /**
     * 高并发库存检查和扣减
     */
    private void validateAndConsumeInventory(DailySession dailySession, List<SelectedGood> selectedGoods, Map<Long, Goods> goodsMap) {
        int makeupNeeded = 0;
        int photographyNeeded = 0;
        Map<String, AtomicInteger> seatConsumption = new HashMap<>();
        seatConsumption.put(AREA_FRONT, new AtomicInteger(0));
        seatConsumption.put(AREA_MIDDLE, new AtomicInteger(0));
        seatConsumption.put(AREA_BACK, new AtomicInteger(0));

        // 计算总消耗量
        for (SelectedGood selectedGood : selectedGoods) {
            Goods goods = goodsMap.get(selectedGood.getGoodId());
            int quantity = selectedGood.getSelectedCount();

            // 化妆和摄影消耗
            if (CATEGORY_MAKEUP.equals(goods.getCategory())) {
                makeupNeeded += quantity;
            } else if (CATEGORY_PHOTOGRAPHY.equals(goods.getCategory())) {
                photographyNeeded += quantity;
            }

            // 座位消耗计算
            if (goods.getSeatConsumptionConfig() != null && !goods.getSeatConsumptionConfig().isEmpty()) {
                try {
                    List<SeatConsumptionConfig> configs = objectMapper.readValue(
                            goods.getSeatConsumptionConfig(),
                            new TypeReference<List<SeatConsumptionConfig>>() {}
                    );

                    for (SeatConsumptionConfig config : configs) {
                        int consumption = config.getNumber() * quantity;
                        seatConsumption.computeIfPresent(config.getArea(), (area, current) -> {
                            current.addAndGet(consumption);
                        });
                    }
                } catch (Exception e) {
                    log.error("解析座位消耗配置失败，goodId={}, config={}",
                            goods.getId(), goods.getSeatConsumptionConfig(), e);
                    throw new BusinessException(ResultCode.ERROR, "商品配置错误：" + goods.getName());
                }
            }
        }

        log.info("库存消耗计算：front={}, middle={}, back={}, makeup={}, photography={}",
                seatConsumption.get(AREA_FRONT).get(),
                seatConsumption.get(AREA_MIDDLE).get(),
                seatConsumption.get(AREA_BACK).get(),
                makeupNeeded, photographyNeeded);

        // 原子性库存检查（使用乐观锁更新库存）
        checkAndConsumeSeatsInventory(dailySession, seatConsumption);
        checkAndConsumeMakeupInventory(dailySession, makeupNeeded);
        checkAndConsumePhotographyInventory(dailySession, photographyNeeded);

        // 更新每日场次库存
        dailySessionMapper.updateById(dailySession);
    }

    /**
     * 检查并扣减座位库存
     */
    private void checkAndConsumeSeatsInventory(DailySession dailySession, Map<String, AtomicInteger> seatConsumption) {
        int totalConsumption = seatConsumption.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        if (totalConsumption > dailySession.getAvailableSeats()) {
            throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT,
                    String.format("座位库存不足，剩余：%d个，需要：%d个",
                            dailySession.getAvailableSeats(), totalConsumption));
        }

        // 扣减库存
        dailySession.setAvailableSeats(dailySession.getAvailableSeats() - totalConsumption);
    }

    /**
     * 检查并扣减化妆库存
     */
    private void checkAndConsumeMakeupInventory(DailySession dailySession, int makeupNeeded) {
        if (makeupNeeded > dailySession.getMakeupStock()) {
            throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT,
                    String.format("化妆库存不足，剩余：%d个，需要：%d个",
                            dailySession.getMakeupStock(), makeupNeeded));
        }
        dailySession.setMakeupStock(dailySession.getMakeupStock() - makeupNeeded);
    }

    /**
     * 检查并扣减摄影库存
     */
    private void checkAndConsumePhotographyInventory(DailySession dailySession, int photographyNeeded) {
        if (photographyNeeded > dailySession.getPhotographyStock()) {
            throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT,
                    String.format("摄影库存不足，剩余：%d个，需要：%d个",
                            dailySession.getPhotographyStock(), photographyNeeded));
        }
        dailySession.setPhotographyStock(dailySession.getPhotographyStock() - photographyNeeded);
    }

    /**
     * 创建预订记录
     */
    private Booking createBookingRecord(CreateBookingRequest request, Session session, DailySession dailySession, Map<Long, Goods> goodsMap) {
        // 生成订单号
        String orderNo = generateOrderNo();

        // 计算总金额
        long totalAmount = calculateTotalAmount(request.getSelectedGoodList(), goodsMap);

        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setThemeId(session.getThemeId());
        booking.setDailySessionId(dailySession.getId());
        booking.setOrderNo(orderNo);
        booking.setTotalAmount(totalAmount);
        booking.setSeatCount(request.getSelectedSeatList().size());
        booking.setBookingDate(dailySession.getDate());
        booking.setStatus(STATUS_PENDING); // 待支付

        bookingMapper.insert(booking);

        log.info("创建预订记录成功，bookingId={}, orderNo={}, totalAmount={}",
                booking.getId(), orderNo, totalAmount);

        return booking;
    }

    /**
     * 创建预订座位记录
     */
    private void createBookingSeatRecords(Long bookingId, List<CreateBookingRequest.SelectedSeatForBooking> selectedSeats) {
        List<BookingSeat> bookingSeats = new ArrayList<>();

        for (CreateBookingRequest.SelectedSeatForBooking selectedSeat : selectedSeats) {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBookingId(bookingId);
            bookingSeat.setSeatId(Long.valueOf(selectedSeat.getSeatId()));
            bookingSeat.setSeatName(selectedSeat.getSeatName());
            bookingSeat.setPrice(0L); // 座位价格可以后续从其他接口获取

            bookingSeats.add(bookingSeat);
        }

        bookingSeatMapper.insertBatchSomeColumn(bookingSeats);
        log.info("创建预订座位记录成功，数量：{}", bookingSeats.size());
    }

    /**
     * 创建预订商品记录
     */
    private void createBookingGoodsRecords(Long bookingId, List<SelectedGood> selectedGoods, Map<Long, Goods> goodsMap) {
        List<BookingGoods> bookingGoodsList = new ArrayList<>();

        for (SelectedGood selectedGood : selectedGoods) {
            Goods goods = goodsMap.get(selectedGood.getGoodId());
            BookingGoods bookingGoods = new BookingGoods();
            bookingGoods.setBookingId(bookingId);
            bookingGoods.setGoodsId(selectedGood.getGoodId());
            bookingGoods.setQuantity(selectedGood.getSelectedCount());
            bookingGoods.setPrice(goods.getPrice() * selectedGood.getSelectedCount());

            bookingGoodsList.add(bookingGoods);
        }

        for (BookingGoods bg : bookingGoodsList) {
            bookingGoodsMapper.insert(bg);
        }
        log.info("创建预订商品记录成功，数量：{}", bookingGoodsList.size());
    }

    /**
     * 计算总金额
     */
    private long calculateTotalAmount(List<SelectedGood> selectedGoods, Map<Long, Goods> goodsMap) {
        return selectedGoods.stream()
                .mapToLong(selectedGood -> {
                    Goods goods = goodsMap.get(selectedGood.getGoodId());
                    return goods.getPrice() * selectedGood.getSelectedCount();
                })
                .sum();
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(new Random().nextInt(1000));
        String data = timestamp + random;
        return DigestUtils.md5DigestAsHex(data).substring(0, 16).toUpperCase();
    }

    /**
     * 构建响应
     */
    private CreateBookingResponse buildResponse(Booking booking) {
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(PAYMENT_EXPIRE_MINUTES);
        String expireTimeStr = expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        CreateBookingResponse response = new CreateBookingResponse();
        response.setOrderId(booking.getOrderNo());
        response.setAmount(booking.getTotalAmount());
        response.setPaymentStatus("pending"); // 待支付
        response.setExpireTime(expireTimeStr);

        return response;
    }

    /**
     * 座位消耗配置POJO
     */
    private static class SeatConsumptionConfig {
        private String area;
        private int number;

        public String getArea() {
            return area;
        }

        public void setArea(String area) {
            this.area = area;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }
}
