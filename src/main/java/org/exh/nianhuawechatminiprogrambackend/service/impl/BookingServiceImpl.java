package org.exh.nianhuawechatminiprogrambackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreateBookingRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedGood;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedSeat;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreateBookingResponse;
import org.exh.nianhuawechatminiprogrambackend.entity.*;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.*;
import org.exh.nianhuawechatminiprogrambackend.service.BookingService;
import org.exh.nianhuawechatminiprogrambackend.util.SeatLockManager;
import org.exh.nianhuawechatminiprogrambackend.util.IdGenerator;
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
