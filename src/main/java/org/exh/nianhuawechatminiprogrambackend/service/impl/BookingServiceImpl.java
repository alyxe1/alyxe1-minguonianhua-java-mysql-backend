package org.exh.nianhuawechatminiprogrambackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.BookingDetailRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreateBookingRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedGood;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedSeat;
import org.exh.nianhuawechatminiprogrambackend.dto.request.UserBookingListRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.*;
import org.exh.nianhuawechatminiprogrambackend.entity.*;
import org.exh.nianhuawechatminiprogrambackend.enums.BookingStatus;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.*;
import org.exh.nianhuawechatminiprogrambackend.service.BookingService;
import org.exh.nianhuawechatminiprogrambackend.util.SeatLockManager;
import org.exh.nianhuawechatminiprogrambackend.util.IdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预订服务实现类
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
    private SeatMapper seatMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private BookingSeatMapper bookingSeatMapper;

    @Autowired
    private BookingGoodsMapper bookingGoodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ThemeMapper themeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SeatLockManager seatLockManager;

    private static final String AREA_FRONT = "front";
    private static final String AREA_MIDDLE = "middle";
    private static final String AREA_BACK = "back";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateBookingResponse createBooking(CreateBookingRequest request) {
        log.info("创建预订开始，userId={}, sessionType={}, date={}, goodsCount={}, seatCount={}",
                request.getUserId(), request.getSessionType(), request.getDate(),
                request.getSelectedGoodList().size(), request.getSelectedSeatList().size());

        try {
            // 1. 参数验证
            validateRequest(request);

            // 2. 解析日期
            LocalDate bookingDate = LocalDate.parse(request.getDate());

            // 3. 查询场次模板
            Session session = getSessionByType(request.getSessionType());
            if (session == null) {
                log.error("场次不存在，sessionType={}", request.getSessionType());
                throw new BusinessException(ResultCode.SESSION_NOT_FOUND, "场次不存在");
            }

            // 4. 查询每日场次
            DailySession dailySession = dailySessionMapper.selectBySessionIdAndDate(
                    session.getId(), bookingDate
            );
            if (dailySession == null) {
                log.error("该日期场次不存在，sessionId={}, date={}", session.getId(), bookingDate);
                throw new BusinessException(ResultCode.SESSION_NOT_FOUND, "该日期场次不存在");
            }

            // 5. 获取商品信息
            List<Long> goodsIds = request.getSelectedGoodList().stream()
                    .map(SelectedGood::getGoodId)
                    .collect(Collectors.toList());
            List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIds);
            if (goodsList.isEmpty()) {
                log.error("未找到任何商品");
                throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
            }

            // 6. 获取选中的座位信息
            List<Long> seatIds = request.getSelectedSeatList().stream()
                    .map(seat -> Long.valueOf(seat.getSeatId()))
                    .collect(Collectors.toList());
            List<Seat> selectedSeats = seatMapper.selectBatchIds(seatIds);
            if (selectedSeats.isEmpty()) {
                log.error("未找到任何座位");
                throw new BusinessException(ResultCode.NOT_FOUND, "座位不存在");
            }

            // 7. 区域匹配验证（关键步骤）
            validateAreaMatch(request.getSelectedGoodList(), goodsList, selectedSeats);

            // 8. 并发锁定座位（核心并发控制）
            List<String> lockedSeats = lockSeats(session.getId(), request.getSelectedSeatList(), request.getUserId());

            // 9. 校验库存充足（防止超卖）
            validateInventory(dailySession, request.getSelectedGoodList(), goodsList);

            // 10. 生成订单号
            String orderNo = IdGenerator.generateOrderNo();

            // 11. 计算总金额（分）
            Long totalAmountInFen = calculateTotalAmount(request.getSelectedGoodList(), goodsList);

            // 12. 转换为元（响应需要元为单位）
            Long totalAmountInYuan = totalAmountInFen / 100;

            // 13. 创建预订记录（数据库中存储分）
            Booking booking = new Booking();
            booking.setUserId(request.getUserId());
            booking.setThemeId(session.getThemeId());
            booking.setDailySessionId(dailySession.getId());
            booking.setOrderNo(orderNo);
            booking.setTotalAmount(totalAmountInFen); // 数据库存储分
            booking.setSeatCount(request.getSelectedSeatList().size());
            booking.setBookingDate(bookingDate);
            booking.setStatus(0); // 0-待支付

            bookingMapper.insert(booking);
            log.info("创建预订记录成功，bookingId={}", booking.getId());

            // 13. 创建预订座位关联
            createBookingSeats(booking.getId(), request.getSelectedSeatList(), selectedSeats);

            // 14. 创建预订商品关联
            createBookingGoods(booking.getId(), request.getSelectedGoodList(), goodsList);

            // 15. 扣减库存（使用乐观锁防止超卖）
            reduceInventory(dailySession, request.getSelectedGoodList(), goodsList);

            // 16. 构建响应（返回元为单位）
            CreateBookingResponse response = new CreateBookingResponse();
            response.setBookingId(String.valueOf(booking.getId()));
            response.setAmount(totalAmountInYuan); // 响应返回元
            response.setPaymentStatus("UNPAID");
            response.setExpireTime(LocalDateTime.now().plusMinutes(10).toString());

            log.info("创建预订成功，bookingId={}, amount={}元", booking.getId(), totalAmountInYuan);
            return response;

        } catch (Exception e) {
            log.error("创建预订失败，userId={}", request.getUserId(), e);
            throw e;
        }
    }

    private void validateRequest(CreateBookingRequest request) {
        if (request.getSessionType() == null || request.getSessionType().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "场次类型不能为空");
        }
        if (request.getDate() == null || request.getDate().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期不能为空");
        }
        if (request.getUserId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (request.getSelectedGoodList() == null || request.getSelectedGoodList().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少选择一个商品");
        }
        if (request.getSelectedSeatList() == null || request.getSelectedSeatList().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少选择一个座位");
        }
    }

    private Session getSessionByType(String sessionType) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<Session>()
                        .eq(Session::getSessionType, sessionType)
                        .eq(Session::getStatus, 1)
                        .eq(Session::getIsDeleted, 0)
        );
    }

    private void validateAreaMatch(List<SelectedGood> selectedGoods, List<Goods> goodsList,
                                   List<Seat> selectedSeats) {
        // 计算各区域需要的座位数
        Map<String, Integer> requiredSeats = new HashMap<>();
        requiredSeats.put(AREA_FRONT, 0);
        requiredSeats.put(AREA_MIDDLE, 0);
        requiredSeats.put(AREA_BACK, 0);

        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        for (SelectedGood good : selectedGoods) {
            Goods goods = goodsMap.get(good.getGoodId());
            if (goods == null || goods.getSeatConsumptionConfig() == null) {
                continue;
            }

            try {
                // 解析JSON配置
                List<AreaConfig> configs = objectMapper.readValue(
                        goods.getSeatConsumptionConfig(),
                        new TypeReference<List<AreaConfig>>() {}
                );

                int quantity = good.getSelectedCount();
                for (AreaConfig config : configs) {
                    String area = config.getArea();
                    int count = config.getNumber() * quantity;
                    requiredSeats.put(area, requiredSeats.get(area) + count);
                }
            } catch (Exception e) {
                log.error("解析座位消耗配置失败，goodId={}", good.getGoodId(), e);
                throw new BusinessException(ResultCode.ERROR, "商品配置错误：" + goods.getName());
            }
        }

        // 计算各区域实际选择的座位数
        Map<String, Integer> actualSeats = new HashMap<>();
        actualSeats.put(AREA_FRONT, 0);
        actualSeats.put(AREA_MIDDLE, 0);
        actualSeats.put(AREA_BACK, 0);

        for (Seat seat : selectedSeats) {
            String area = seat.getSeatType();
            actualSeats.put(area, actualSeats.get(area) + 1);
        }

        // 对比验证
        for (String area : Arrays.asList(AREA_FRONT, AREA_MIDDLE, AREA_BACK)) {
            int required = requiredSeats.get(area);
            int actual = actualSeats.get(area);
            if (required != actual) {
                log.error("区域{}座位数量不匹配，需要{}个，实际选择{}个", area, required, actual);
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        String.format("区域%s座位数量不匹配，需要%d个，实际选择%d个", area, required, actual));
            }
        }

        log.info("区域匹配验证通过");
    }

    private List<String> lockSeats(Long sessionId, List<SelectedSeat> selectedSeats, Long userId) {
        List<String> lockedSeats = new ArrayList<>();
        try {
            for (SelectedSeat seat : selectedSeats) {
                String seatId = seat.getSeatId();
                boolean locked = seatLockManager.tryLockSeat(sessionId, seatId, userId);
                if (!locked) {
                    log.error("座位锁定失败，seatId={}", seatId);
                    releaseLockedSeats(lockedSeats, sessionId, userId);
                    throw new BusinessException(ResultCode.SEAT_ALREADY_LOCKED, "座位已被锁定");
                }
                lockedSeats.add(seatId);
            }
            log.info("座位锁定成功，共锁定{}个座位", lockedSeats.size());
            return lockedSeats;
        } catch (Exception e) {
            releaseLockedSeats(lockedSeats, sessionId, userId);
            throw e;
        }
    }

    private void releaseLockedSeats(List<String> lockedSeats, Long sessionId, Long userId) {
        for (String seatId : lockedSeats) {
            seatLockManager.releaseSeatLock(sessionId, seatId, userId);
        }
    }

    private void validateInventory(DailySession dailySession,
                                   List<SelectedGood> selectedGoods,
                                   List<Goods> goodsList) {
        // 计算各类库存消耗
        int makeupConsumption = 0;
        int photographyConsumption = 0;
        int totalSeatConsumption = 0;

        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        for (SelectedGood good : selectedGoods) {
            Goods goods = goodsMap.get(good.getGoodId());
            if (goods == null) continue;

            int quantity = good.getSelectedCount();
            String category = goods.getCategory();

            if ("makeup".equals(category)) {
                makeupConsumption += quantity;
            } else if ("photos".equals(category)) {
                photographyConsumption += quantity;
            }

            // 计算座位消耗
            if (goods.getSeatConsumptionConfig() != null) {
                try {
                    List<AreaConfig> configs = objectMapper.readValue(
                            goods.getSeatConsumptionConfig(),
                            new TypeReference<List<AreaConfig>>() {}
                    );
                    for (AreaConfig config : configs) {
                        totalSeatConsumption += config.getNumber() * quantity;
                    }
                } catch (Exception e) {
                    log.error("解析座位消耗配置失败", e);
                }
            }
        }

        // 验证库存
        if (makeupConsumption > 0 && dailySession.getMakeupStock() < makeupConsumption) {
            log.error("化妆库存不足，剩余：{}，需要：{}", dailySession.getMakeupStock(), makeupConsumption);
            throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT, "化妆库存不足");
        }

        if (photographyConsumption > 0 && dailySession.getPhotographyStock() < photographyConsumption) {
            log.error("摄影库存不足，剩余：{}，需要：{}", dailySession.getPhotographyStock(), photographyConsumption);
            throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT, "摄影库存不足");
        }

        if (totalSeatConsumption > 0 && dailySession.getAvailableSeats() < totalSeatConsumption) {
            log.error("座位库存不足，剩余：{}，需要：{}", dailySession.getAvailableSeats(), totalSeatConsumption);
            throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT, "座位库存不足");
        }

        log.info("库存验证通过");
    }

    private Long calculateTotalAmount(List<SelectedGood> selectedGoods, List<Goods> goodsList) {
        long totalAmount = 0L;
        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        for (SelectedGood good : selectedGoods) {
            Goods goods = goodsMap.get(good.getGoodId());
            if (goods != null) {
                int quantity = good.getSelectedCount();
                totalAmount += goods.getPrice() * quantity;
            }
        }

        return totalAmount;
    }

    private void createBookingSeats(Long bookingId, List<SelectedSeat> selectedSeats, List<Seat> seatList) {
        Map<Long, Seat> seatMap = seatList.stream()
                .collect(Collectors.toMap(Seat::getId, s -> s));

        for (SelectedSeat selectedSeat : selectedSeats) {
            Long seatId = Long.valueOf(selectedSeat.getSeatId());
            Seat seat = seatMap.get(seatId);

            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBookingId(bookingId);
            bookingSeat.setSeatId(seatId);
            bookingSeat.setSeatName(selectedSeat.getSeatName());
            bookingSeat.setPrice(seat != null ? seat.getPrice() : 0L);

            bookingSeatMapper.insert(bookingSeat);
        }

        log.info("创建预订座位关联成功，共{}个", selectedSeats.size());
    }

    private void createBookingGoods(Long bookingId, List<SelectedGood> selectedGoods, List<Goods> goodsList) {
        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        for (SelectedGood selectedGood : selectedGoods) {
            Goods goods = goodsMap.get(selectedGood.getGoodId());

            BookingGoods bookingGoods = new BookingGoods();
            bookingGoods.setBookingId(bookingId);
            bookingGoods.setGoodsId(selectedGood.getGoodId());
            bookingGoods.setQuantity(selectedGood.getSelectedCount());
            bookingGoods.setPrice(goods != null ? goods.getPrice() : 0L);

            bookingGoodsMapper.insert(bookingGoods);
        }

        log.info("创建预订商品关联成功，共{}个", selectedGoods.size());
    }

    private void reduceInventory(DailySession dailySession,
                                 List<SelectedGood> selectedGoods,
                                 List<Goods> goodsList) {
        // 使用乐观锁扣减库存
        DailySession updateSession = new DailySession();
        updateSession.setId(dailySession.getId());

        int totalSeatConsumption = 0;
        int makeupConsumption = 0;
        int photographyConsumption = 0;

        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        for (SelectedGood good : selectedGoods) {
            Goods goods = goodsMap.get(good.getGoodId());
            if (goods == null) continue;

            int quantity = good.getSelectedCount();
            String category = goods.getCategory();

            if ("makeup".equals(category)) {
                makeupConsumption += quantity;
            } else if ("photos".equals(category)) {
                photographyConsumption += quantity;
            }

            // 计算座位消耗
            if (goods.getSeatConsumptionConfig() != null) {
                try {
                    List<AreaConfig> configs = objectMapper.readValue(
                            goods.getSeatConsumptionConfig(),
                            new TypeReference<List<AreaConfig>>() {}
                    );
                    for (AreaConfig config : configs) {
                        totalSeatConsumption += config.getNumber() * quantity;
                    }
                } catch (Exception e) {
                    log.error("解析座位消耗配置失败", e);
                }
            }
        }

        // 扣减库存
        updateSession.setAvailableSeats(dailySession.getAvailableSeats() - totalSeatConsumption);
        updateSession.setMakeupStock(dailySession.getMakeupStock() - makeupConsumption);
        updateSession.setPhotographyStock(dailySession.getPhotographyStock() - photographyConsumption);

        int rows = dailySessionMapper.updateById(updateSession);
        if (rows == 0) {
            log.error("库存扣减失败，可能已被其他事务修改");
            throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT, "库存不足");
        }

        log.info("库存扣减成功，座位：{}，化妆：{}，摄影：{}",
                totalSeatConsumption, makeupConsumption, photographyConsumption);
    }

    @Override
    public PageResult<BookingListItemDTO> getUserBookingList(Long userId, UserBookingListRequest request) {
        log.info("查询用户预订列表，userId={}, pageNum={}, pageSize={}, status={}",
                userId, request.getPageNum(), request.getPageSize(), request.getStatus());

        try {
            // 1. 构建查询条件
            LambdaQueryWrapper<Booking> queryWrapper = new LambdaQueryWrapper<Booking>()
                    .eq(Booking::getUserId, userId)
                    .eq(Booking::getIsDeleted, 0)
                    .orderByDesc(Booking::getCreatedAt);

            // 2. 状态筛选
            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                BookingStatus statusEnum = BookingStatus.fromCode(request.getStatus());
                if (statusEnum != null) {
                    queryWrapper.eq(Booking::getStatus, statusEnum.getDbValue());
                }
            }

            // 3. 分页查询
            Page<Booking> page = new Page<>(request.getPageNum(), request.getPageSize());
            IPage<Booking> bookingPage = bookingMapper.selectPage(page, queryWrapper);

            if (bookingPage.getRecords().isEmpty()) {
                log.info("用户暂无预订记录，userId={}", userId);
                return PageResult.of(new ArrayList<>(), 0L, request.getPageNum(), request.getPageSize());
            }

            // 4. 构建结果
            List<BookingListItemDTO> items = new ArrayList<>();
            for (Booking booking : bookingPage.getRecords()) {
                BookingListItemDTO item = convertToBookingListItem(booking);
                items.add(item);
            }

            log.info("查询用户预订列表成功，共{}条记录", bookingPage.getTotal());
            return PageResult.of(items, bookingPage.getTotal(), (int) bookingPage.getCurrent(), (int) bookingPage.getSize());

        } catch (Exception e) {
            log.error("查询用户预订列表失败，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 转换Booking为BookingListItemDTO
     */
    private BookingListItemDTO convertToBookingListItem(Booking booking) {
        BookingListItemDTO dto = new BookingListItemDTO();
        dto.setId(booking.getId());
        dto.setBookingId(String.valueOf(booking.getId()));
        dto.setAmount(convertToYuan(booking.getTotalAmount()));

        // 设置状态
        BookingStatus statusEnum = BookingStatus.fromDbValue(booking.getStatus());
        dto.setStatus(statusEnum != null ? statusEnum.getCode() : "unknown");
        dto.setStatusName(statusEnum != null ? statusEnum.getName() : "未知");

        dto.setCreatedAt(booking.getCreatedAt());

        // 查询关联信息
        fillOrderInfo(dto, booking);
        fillBookingInfo(dto, booking);

        return dto;
    }

    /**
     * 填充订单信息
     */
    private void fillOrderInfo(BookingListItemDTO dto, Booking booking) {
        try {
            // 查询订单信息
            LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<Order>()
                    .eq(Order::getBookingId, booking.getId())
                    .orderByDesc(Order::getCreatedAt)
                    .last("LIMIT 1");

            Order order = orderMapper.selectOne(orderQuery);
            if (order != null) {
                // 设置支付方式和支付时间
                if (order.getPaymentMethod() != null && !order.getPaymentMethod().isEmpty()) {
                    dto.setPaymentMethod(order.getPaymentMethod());
                }
                if (order.getPaymentTime() != null && !order.getPaymentTime().equals("1970-01-01 00:00:00")) {
                    dto.setPaymentTime(order.getPaymentTime());
                }
            }
        } catch (Exception e) {
            log.error("填充订单信息失败，bookingId={}", booking.getId(), e);
        }
    }

    /**
     * 填充预订信息（主题、场次、人数）
     */
    private void fillBookingInfo(BookingListItemDTO dto, Booking booking) {
        try {
            BookingInfoDTO bookingInfo = new BookingInfoDTO();

            // 查询主题信息
            Theme theme = themeMapper.selectById(booking.getThemeId());
            if (theme != null) {
                bookingInfo.setThemeTitle(theme.getTitle());
                bookingInfo.setThemePic(theme.getCoverImage());
            }

            // 查询每日场次信息
            DailySession dailySession = dailySessionMapper.selectById(booking.getDailySessionId());
            if (dailySession != null) {
                // 查询场次模板
                Session session = sessionMapper.selectById(dailySession.getSessionId());
                if (session != null) {
                    // 拼接场次时间：日期 + 开始时间
                    LocalDateTime sessionTime = LocalDateTime.of(
                            booking.getBookingDate(),
                            session.getStartTime()
                    );
                    bookingInfo.setSessionTime(sessionTime);
                }
            }

            // 设置人数（座位数量）
            bookingInfo.setPeopleCount(booking.getSeatCount());

            dto.setBookingInfo(bookingInfo);
        } catch (Exception e) {
            log.error("填充预订信息失败，bookingId={}", booking.getId(), e);
        }
    }

    @Override
    public BookingDetailResponse getBookingDetail(BookingDetailRequest request) {
        log.info("查询预订详情，bookingId={}, userId={}", request.getBookingId(), request.getUserId());

        try {
            // 1. 查询预订信息
            Long bookingId = Long.valueOf(request.getBookingId());
            Booking booking = bookingMapper.selectById(bookingId);
            if (booking == null) {
                log.error("预订不存在，bookingId={}", bookingId);
                throw new RuntimeException("预订不存在");
            }

            // 2. 验证用户权限
            if (!booking.getUserId().toString().equals(request.getUserId())) {
                log.error("用户无权访问该预订，bookingUserId={}, requestUserId={}",
                        booking.getUserId(), request.getUserId());
                throw new RuntimeException("无权访问该预订");
            }

            // 3. 构建响应
            BookingDetailResponse response = new BookingDetailResponse();

            // 4. 构建预订详情
            BookingDetailDTO bookingDetail = buildBookingDetail(booking);
            response.setBookingDetail(bookingDetail);

            // 5. 查询并设置主题相关信息
            fillThemeInfo(response, booking);

            log.info("查询预订详情成功，bookingId={}", bookingId);
            return response;

        } catch (Exception e) {
            log.error("查询预订详情失败，bookingId={}", request.getBookingId(), e);
            throw e;
        }
    }

    /**
     * 构建预订详情
     */
    private BookingDetailDTO buildBookingDetail(Booking booking) {
        BookingDetailDTO dto = new BookingDetailDTO();
        dto.setOrderId(String.valueOf(booking.getId()));
        dto.setDate(booking.getBookingDate().toString());

        // 设置支付状态
        BookingStatus statusEnum = BookingStatus.fromDbValue(booking.getStatus());
        if (statusEnum != null) {
            dto.setPaymentStatus(statusEnum.getCode());
        } else {
            dto.setPaymentStatus("unknown");
        }

        // 设置时间戳
        dto.setTimeStamp(String.valueOf(System.currentTimeMillis()));

        // 查询订单信息获取支付状态和时间
        Order order = getOrderByBookingId(booking.getId());
        if (order != null && order.getPaymentTime() != null && !order.getPaymentTime().equals("1970-01-01 00:00:00")) {
            // 已支付
            dto.setPaymentStatus("paid");
        }

        // 设置过期时间（预订创建时间+10分钟）
        java.time.LocalDateTime expireTime = booking.getCreatedAt().plusMinutes(10);
        dto.setExpireTime(expireTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());

        // 查询用户联系电话
        User user = userMapper.selectById(booking.getUserId());
        if (user != null && user.getPhone() != null && !user.getPhone().isEmpty()) {
            dto.setContactPhone(user.getPhone());
        } else {
            dto.setContactPhone("");
        }

        // 查询选中的座位
        dto.setSelectedSeatList(getSelectedSeats(booking.getId()));

        // 查询选中的商品（转换为对象列表）
        dto.setSelectedGoodList(getSelectedGoodsInfo(booking.getId()));

        // 计算参考价格（expectedPrice）- 转换为元
        Long expectedPriceInFen = calculateExpectedPrice(booking.getId());
        Long expectedPriceInYuan = expectedPriceInFen / 100; // 分转换为元
        dto.setExpectedPrice(String.valueOf(expectedPriceInYuan));

        return dto;
    }

    /**
     * 根据bookingId查询订单
     */
    private Order getOrderByBookingId(Long bookingId) {
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<Order>()
                .eq(Order::getBookingId, bookingId)
                .orderByDesc(Order::getCreatedAt)
                .last("LIMIT 1");
        return orderMapper.selectOne(query);
    }

    /**
     * 获取选中的座位列表
     */
    private List<SeatDetailDTO> getSelectedSeats(Long bookingId) {
        List<SeatDetailDTO> seatList = new ArrayList<>();

        LambdaQueryWrapper<BookingSeat> query = new LambdaQueryWrapper<BookingSeat>()
                .eq(BookingSeat::getBookingId, bookingId);

        List<BookingSeat> bookingSeats = bookingSeatMapper.selectList(query);
        for (BookingSeat bookingSeat : bookingSeats) {
            SeatDetailDTO seatDTO = new SeatDetailDTO();
            seatDTO.setSeatId(String.valueOf(bookingSeat.getSeatId()));
            seatDTO.setSeatName(bookingSeat.getSeatName() != null ? bookingSeat.getSeatName() : "");
            seatList.add(seatDTO);
        }

        return seatList;
    }

    /**
     * 获取选中的商品信息列表
     */
    private List<SelectedGoodInfoDTO> getSelectedGoodsInfo(Long bookingId) {
        List<SelectedGoodInfoDTO> goodsInfoList = new ArrayList<>();

        // 查询预订商品关联
        LambdaQueryWrapper<BookingGoods> query = new LambdaQueryWrapper<BookingGoods>()
                .eq(BookingGoods::getBookingId, bookingId);

        List<BookingGoods> bookingGoodsList = bookingGoodsMapper.selectList(query);

        // 构建商品信息列表
        if (!bookingGoodsList.isEmpty()) {
            for (BookingGoods bookingGoods : bookingGoodsList) {
                SelectedGoodInfoDTO goodsInfo = new SelectedGoodInfoDTO();
                goodsInfo.setGoodId(String.valueOf(bookingGoods.getGoodsId()));
                goodsInfo.setSelectedCount(bookingGoods.getQuantity());

                // 查询商品信息获取名称和价格
                Goods goods = goodsMapper.selectById(bookingGoods.getGoodsId());
                if (goods != null) {
                    goodsInfo.setGoodName(goods.getName());
                    // 价格从分转换为元，保留两位小数
                    double priceInYuan = goods.getPrice() / 100.0;
                    goodsInfo.setGoodPrice(String.format("%.2f", priceInYuan));
                    log.debug("商品详情：goodsId={}, name={}, price={}, quantity={}",
                             goods.getId(), goods.getName(), goods.getPrice(), bookingGoods.getQuantity());
                } else {
                    // 商品不存在时设置默认值
                    goodsInfo.setGoodName("未知商品");
                    goodsInfo.setGoodPrice("0.00");
                    log.warn("商品不存在，goodsId={}", bookingGoods.getGoodsId());
                }

                goodsInfoList.add(goodsInfo);
            }
        }

        return goodsInfoList;
    }

    /**
     * 计算参考价格（内部方法，返回分）
     * 基于booking_goods表中的商品数量和goods表中的价格计算
     * @return 价格（分）
     */
    private Long calculateExpectedPrice(Long bookingId) {
        try {
            // 查询预订商品关联
            LambdaQueryWrapper<BookingGoods> query = new LambdaQueryWrapper<BookingGoods>()
                    .eq(BookingGoods::getBookingId, bookingId);

            List<BookingGoods> bookingGoodsList = bookingGoodsMapper.selectList(query);

            if (bookingGoodsList.isEmpty()) {
                log.info("预订没有关联商品，bookingId={}", bookingId);
                return 0L;
            }

            // 计算总价（分）
            long totalPrice = 0L;
            for (BookingGoods bookingGoods : bookingGoodsList) {
                // 查询商品信息获取价格
                Goods goods = goodsMapper.selectById(bookingGoods.getGoodsId());
                if (goods != null) {
                    long goodsTotalPrice = goods.getPrice() * bookingGoods.getQuantity();
                    totalPrice += goodsTotalPrice;
                    log.debug("商品计算：goodsId={}, price={}, quantity={}, subtotal={}",
                            goods.getId(), goods.getPrice(), bookingGoods.getQuantity(), goodsTotalPrice);
                } else {
                    log.warn("未找到商品信息，goodsId={}", bookingGoods.getGoodsId());
                }
            }

            log.info("参考价格计算完成，bookingId={}, expectedPrice={}分", bookingId, totalPrice);
            return totalPrice;

        } catch (Exception e) {
            log.error("计算参考价格失败，bookingId={}", bookingId, e);
            return 0L; // 出错时返回0
        }
    }

    /**
     * 填充主题相关信息
     */
    private void fillThemeInfo(BookingDetailResponse response, Booking booking) {
        // 查询主题信息
        Theme theme = themeMapper.selectById(booking.getThemeId());
        if (theme != null) {
            response.setThemeName(theme.getTitle());
            response.setThemePic(theme.getCoverImage());
            response.setAddress(theme.getAddress());
        }

        // 查询场次类型
        DailySession dailySession = dailySessionMapper.selectById(booking.getDailySessionId());
        if (dailySession != null) {
            Session session = sessionMapper.selectById(dailySession.getSessionId());
            if (session != null) {
                response.setSessionType(session.getSessionType());
            }
        }
    }

    /**
     * 分转换为元
     */
    private java.math.BigDecimal convertToYuan(Long fen) {
        if (fen == null) {
            return java.math.BigDecimal.ZERO;
        }
        return java.math.BigDecimal.valueOf(fen).divide(java.math.BigDecimal.valueOf(100));
    }

    /**
     * 区域配置POJO
     */
    private static class AreaConfig {
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
