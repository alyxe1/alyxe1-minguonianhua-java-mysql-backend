package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.AvailableSessionsResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.SessionItem;
import org.exh.nianhuawechatminiprogrambackend.entity.DailySession;
import org.exh.nianhuawechatminiprogrambackend.entity.Seat;
import org.exh.nianhuawechatminiprogrambackend.entity.Session;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.BookingSeatMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.DailySessionMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SeatMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SessionMapper;
import org.exh.nianhuawechatminiprogrambackend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 场次服务实现类
 * 从daily_sessions表中查询当天的实际库存
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private DailySessionMapper dailySessionMapper;

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private BookingSeatMapper bookingSeatMapper;

    @Override
    @Transactional
    public AvailableSessionsResponse getAvailableSessions(Long themeId, String date) {
        log.info("获取可用场次开始，themeId={}, date={}", themeId, date);

        // 1. 参数校验
        if (themeId == null) {
            log.error("主题ID不能为空");
            throw new BusinessException(ResultCode.ERROR, "主题ID不能为空");
        }

        // 2. 日期处理：如果date为空，使用当前日期
        LocalDate queryDate;
        if (date == null || date.isEmpty()) {
            queryDate = LocalDate.now();
            log.info("日期为空，使用当前日期: {}", queryDate);
        } else {
            // 验证日期格式
            try {
                queryDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                log.error("日期格式错误: {}", date);
                throw new BusinessException(ResultCode.ERROR, "日期格式错误，应为YYYY-MM-DD格式");
            }
        }

        // 3. 查询主题下的所有场次模板
        List<Session> sessionList = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                        .eq(Session::getThemeId, themeId)
                        .eq(Session::getStatus, 1)
                        .eq(Session::getIsDeleted, 0)
                        .orderByAsc(Session::getStartTime)
        );
        log.info("查询到{}个场次模板", sessionList.size());

        // 4. 组装响应数据
        List<SessionItem> items = new ArrayList<>();
        for (Session session : sessionList) {
            // 查询或创建每日场次实例
            DailySession dailySession = getOrCreateDailySession(session, queryDate);

            SessionItem item = new SessionItem();
            item.setTitle(session.getSessionName() + " " +
                         session.getStartTime() + "-" + session.getEndTime());

            // 创建descList（按区域显示剩余座位数）
            List<String> descList = new ArrayList<>();

            // 1. 查询该场次的所有座位，按seat_type分组统计
            List<Seat> allSeats = seatMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                            .eq(Seat::getSessionTemplateId, session.getId())
            );

            // 按区域统计总座位数
            Map<String, Long> totalSeatsByArea = new HashMap<>();
            for (Seat seat : allSeats) {
                String seatType = seat.getSeatType();
                if (seatType == null) {
                    seatType = "front"; // 默认值为front
                }
                totalSeatsByArea.put(seatType, totalSeatsByArea.getOrDefault(seatType, 0L) + 1);
            }

            log.debug("场次ID={}, 各区域总座位数={}", session.getId(), totalSeatsByArea);

            // 2. 查询该场次该日期下已预订的座位（排除已取消的预订）
            List<Long> bookedSeatIds = bookingSeatMapper.selectBookedSeatIds(session.getId(), queryDate);
            log.debug("场次ID={}, 日期={}, 已预订座位数={}", session.getId(), queryDate, bookedSeatIds.size());

            // 3. 按区域统计已预订座位数
            Map<String, Long> bookedSeatsByArea = new HashMap<>();
            for (Long seatId : bookedSeatIds) {
                // 查询座位的seat_type
                Seat seat = seatMapper.selectById(seatId);
                if (seat != null) {
                    String seatType = seat.getSeatType();
                    if (seatType == null) {
                        seatType = "front";
                    }
                    bookedSeatsByArea.put(seatType, bookedSeatsByArea.getOrDefault(seatType, 0L) + 1);
                }
            }

            log.debug("场次ID={}, 各区域已预订座位数={}", session.getId(), bookedSeatsByArea);

            // 4. 计算并生成descList（按front、middle、back顺序）
            String[] areaOrder = {"front", "middle", "back"};
            String[] areaNames = {"内场", "中场", "外场"};

            for (int i = 0; i < areaOrder.length; i++) {
                String area = areaOrder[i];
                String areaName = areaNames[i];

                long total = totalSeatsByArea.getOrDefault(area, 0L);
                long booked = bookedSeatsByArea.getOrDefault(area, 0L);
                long remaining = total - booked;

                descList.add(areaName + " 余 " + remaining);
            }

            item.setDescList(descList);
            items.add(item);
        }

        AvailableSessionsResponse response = new AvailableSessionsResponse();
        response.setItems(items);

        log.info("获取可用场次完成，共{}个场次", items.size());
        return response;
    }

    /**
     * 获取或创建每日场次实例
     * 如果不存在，则从模板创建
     */
    private DailySession getOrCreateDailySession(Session session, LocalDate date) {
        // 查询daily_sessions表
        DailySession dailySession = dailySessionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DailySession>()
                        .eq(DailySession::getSessionId, session.getId())
                        .eq(DailySession::getDate, date)
        );

        // 如果不存在，创建新的每日场次实例
        if (dailySession == null) {
            log.info("创建新的每日场次实例，sessionId={}, date={}", session.getId(), date);

            dailySession = new DailySession();
            dailySession.setSessionId(session.getId());
            dailySession.setDate(date);
            dailySession.setAvailableSeats(session.getTotalSeats());
            dailySession.setMakeupStock(session.getTotalMakeup());
            dailySession.setPhotographyStock(session.getTotalPhotography());

            dailySessionMapper.insert(dailySession);
        }

        return dailySession;
    }
}
