package org.exh.nianhuawechatminiprogrambackend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CheckSeatRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedGood;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CheckSeatResponse;
import org.exh.nianhuawechatminiprogrambackend.entity.DailySession;
import org.exh.nianhuawechatminiprogrambackend.entity.Goods;
import org.exh.nianhuawechatminiprogrambackend.entity.Session;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.DailySessionMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.GoodsMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SessionMapper;
import org.exh.nianhuawechatminiprogrambackend.service.CheckSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 座位校验服务实现类
 */
@Slf4j
@Service
public class CheckSeatServiceImpl implements CheckSeatService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private DailySessionMapper dailySessionMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 座位区域常量
     */
    private static final String AREA_FRONT = "front";
    private static final String AREA_MIDDLE = "middle";
    private static final String AREA_BACK = "back";

    /**
     * 商品分类常量
     */
    private static final String CATEGORY_MAKEUP = "makeup";
    private static final String CATEGORY_PHOTOGRAPHY = "photos";

    @Override
    public CheckSeatResponse checkSeats(CheckSeatRequest request) {
        log.info("开始校验座位，date={}, sessionType={}, goodsCount={}",
                request.getDate(), request.getSessionType(), request.getSelectedGoodList().size());

        // 1. 参数基本校验
        if (request.getDate() == null || request.getDate().isEmpty()) {
            log.error("日期不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期不能为空");
        }

        if (request.getSessionType() == null || request.getSessionType().isEmpty()) {
            log.error("场次类型不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "场次类型不能为空");
        }

        if (request.getSelectedGoodList() == null || request.getSelectedGoodList().isEmpty()) {
            log.error("商品列表不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少选择一个商品");
        }

        // 2. 解析日期
        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(request.getDate());
        } catch (Exception e) {
            log.error("日期格式错误，应为YYYY-MM-DD格式: {}", request.getDate());
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

        // 5. 获取商品信息
        List<Long> goodsIds = request.getSelectedGoodList().stream()
                .map(SelectedGood::getGoodId)
                .collect(Collectors.toList());

        List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIds);
        if (goodsList.isEmpty()) {
            log.error("未找到任何商品");
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }

        // 6. 构建商品ID到商品对象的映射
        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        // 7. 计算各区域和各类别消耗总量
        SeatConsumption consumption = calculateConsumption(request.getSelectedGoodList(), goodsMap);

        // 8. 验证库存是否足够
        validateInventory(dailySession, consumption, session, bookingDate);

        log.info("座位校验通过，date={}, sessionType={}", request.getDate(), request.getSessionType());

        // 9. 构建响应，原样返回商品列表
        CheckSeatResponse response = new CheckSeatResponse();
        response.setSelectedGoodList(request.getSelectedGoodList());

        return response;
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
    private DailySession getDailySession(Long sessionId, LocalDate date) {
        return dailySessionMapper.selectBySessionIdAndDate(sessionId, date);
    }

    /**
     * 计算各类库存消耗
     */
    private SeatConsumption calculateConsumption(List<SelectedGood> selectedGoods, Map<Long, Goods> goodsMap) {
        SeatConsumption consumption = new SeatConsumption();

        for (SelectedGood selectedGood : selectedGoods) {
            Goods goods = goodsMap.get(selectedGood.getGoodId());
            if (goods == null) {
                log.error("商品不存在，goodId={}", selectedGood.getGoodId());
                throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在：" + selectedGood.getGoodId());
            }

            Integer quantity = selectedGood.getSelectedCount();
            String category = goods.getCategory();

            // 处理化妆和摄影库存消耗
            if (CATEGORY_MAKEUP.equals(category)) {
                consumption.addMakeup(quantity);
            } else if (CATEGORY_PHOTOGRAPHY.equals(category)) {
                consumption.addPhotography(quantity);
            }

            // 处理座位区域消耗（所有商品类型都可能消耗座位）
            if (goods.getSeatConsumptionConfig() != null && !goods.getSeatConsumptionConfig().isEmpty()) {
                try {
                    List<SeatConsumptionConfig> configs = objectMapper.readValue(
                            goods.getSeatConsumptionConfig(),
                            new TypeReference<List<SeatConsumptionConfig>>() {}
                    );

                    for (SeatConsumptionConfig config : configs) {
                        int areaConsumption = config.getNumber() * quantity;
                        switch (config.getArea()) {
                            case AREA_FRONT:
                                consumption.addFront(areaConsumption);
                                break;
                            case AREA_MIDDLE:
                                consumption.addMiddle(areaConsumption);
                                break;
                            case AREA_BACK:
                                consumption.addBack(areaConsumption);
                                break;
                            default:
                                log.warn("未知的座位区域：{}", config.getArea());
                        }
                    }
                } catch (Exception e) {
                    log.error("解析座位消耗配置失败，goodId={}, config={}",
                            goods.getId(), goods.getSeatConsumptionConfig(), e);
                    throw new BusinessException(ResultCode.ERROR, "商品配置错误：" + goods.getName());
                }
            }
        }

        log.info("库存消耗计算完成：front={}, middle={}, back={}, makeup={}, photography={}",
                consumption.getFront(), consumption.getMiddle(), consumption.getBack(),
                consumption.getMakeup(), consumption.getPhotography());

        return consumption;
    }

    /**
     * 验证库存是否足够
     */
    private void validateInventory(DailySession dailySession, SeatConsumption consumption,
                                   Session session, LocalDate bookingDate) {
        // 验证化妆库存
        if (consumption.getMakeup() > 0) {
            if (dailySession.getMakeupStock() < consumption.getMakeup()) {
                log.error("化妆库存不足，剩余：{}, 需要：{}", dailySession.getMakeupStock(), consumption.getMakeup());
                throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT, "化妆库存不足");
            }
        }

        // 验证摄影库存
        if (consumption.getPhotography() > 0) {
            if (dailySession.getPhotographyStock() < consumption.getPhotography()) {
                log.error("摄影库存不足，剩余：{}, 需要：{}", dailySession.getPhotographyStock(), consumption.getPhotography());
                throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT, "摄影库存不足");
            }
        }

        // 验证座位区域库存
        // 由于座位是按区域分配的，我们需要检查每个区域的实际可用数量
        // 这里简化处理：检查每个区域的消耗是否不超过总库存
        // 在实际场景中，应该从seats表中查询每个区域的实际可用数量

        int totalSeatConsumption = consumption.getFront() + consumption.getMiddle() + consumption.getBack();

        if (totalSeatConsumption > 0) {
            if (dailySession.getAvailableSeats() < totalSeatConsumption) {
                log.error("座位库存不足，剩余：{}, 需要：{}", dailySession.getAvailableSeats(), totalSeatConsumption);
                throw new BusinessException(ResultCode.INVENTORY_INSUFFICIENT,
                        String.format("座位库存不足，剩余%d个，需要%d个", dailySession.getAvailableSeats(), totalSeatConsumption));
            }
        }

        log.info("库存验证通过，所有库存充足");
    }

    /**
     * 库存消耗汇总类
     */
    private static class SeatConsumption {
        private int front = 0;
        private int middle = 0;
        private int back = 0;
        private int makeup = 0;
        private int photography = 0;

        public void addFront(int count) {
            this.front += count;
        }

        public void addMiddle(int count) {
            this.middle += count;
        }

        public void addBack(int count) {
            this.back += count;
        }

        public void addMakeup(int count) {
            this.makeup += count;
        }

        public void addPhotography(int count) {
            this.photography += count;
        }

        public int getFront() {
            return front;
        }

        public int getMiddle() {
            return middle;
        }

        public int getBack() {
            return back;
        }

        public int getMakeup() {
            return makeup;
        }

        public int getPhotography() {
            return photography;
        }
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
