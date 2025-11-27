package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.AvailableSessionsResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.SessionItem;
import org.exh.nianhuawechatminiprogrambackend.entity.DailySession;
import org.exh.nianhuawechatminiprogrambackend.entity.Session;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.DailySessionMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SessionMapper;
import org.exh.nianhuawechatminiprogrambackend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

            // 创建descList
            List<String> descList = new ArrayList<>();

            // 席位余量
            Integer availableSeats = dailySession.getAvailableSeats();
            if (availableSeats == null) {
                availableSeats = 0;
            }
            descList.add("席位 余 " + availableSeats);

            // 化妆余量
            Integer makeupStock = dailySession.getMakeupStock();
            if (makeupStock == null) {
                makeupStock = 0;
            }
            descList.add("化妆 余 " + makeupStock);

            // 摄影余量
            Integer photographyStock = dailySession.getPhotographyStock();
            if (photographyStock == null) {
                photographyStock = 0;
            }
            descList.add("摄影 余 " + photographyStock);

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
