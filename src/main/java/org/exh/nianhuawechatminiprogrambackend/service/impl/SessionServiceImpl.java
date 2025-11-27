package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.AvailableSessionsResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.SessionItem;
import org.exh.nianhuawechatminiprogrambackend.entity.Session;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.SessionMapper;
import org.exh.nianhuawechatminiprogrambackend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 场次服务实现类
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionMapper sessionMapper;

    @Override
    public AvailableSessionsResponse getAvailableSessions(Long themeId, String date) {
        log.info("获取可用场次开始，themeId={}, date={}", themeId, date);

        // 1. 参数校验
        if (themeId == null) {
            log.error("主题ID不能为空");
            throw new BusinessException(ResultCode.ERROR, "主题ID不能为空");
        }

        // 2. 日期处理：如果date为空，使用当前日期
        String queryDate;
        if (date == null || date.isEmpty()) {
            queryDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.info("日期为空，使用当前日期: {}", queryDate);
        } else {
            // 验证日期格式
            try {
                LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                queryDate = date;
            } catch (Exception e) {
                log.error("日期格式错误: {}", date);
                throw new BusinessException(ResultCode.ERROR, "日期格式错误，应为YYYY-MM-DD格式");
            }
        }

        // 3. 查询可用场次
        List<Session> sessionList = sessionMapper.selectAvailableSessions(themeId, queryDate);
        log.info("查询到{}个可用场次", sessionList.size());

        // 4. 组装响应数据
        List<SessionItem> items = new ArrayList<>();
        for (Session session : sessionList) {
            SessionItem item = new SessionItem();
            item.setTitle(session.getSessionName());

            // 创建descList
            List<String> descList = new ArrayList<>();

            // 席位余量
            Integer availableSeats = session.getAvailableSeats();
            if (availableSeats == null) {
                availableSeats = 0;
            }
            descList.add("席位 余 " + availableSeats);

            // 化妆余量
            Integer makeupStock = session.getMakeupStock();
            if (makeupStock == null) {
                makeupStock = 0;
            }
            descList.add("化妆 余 " + makeupStock);

            // 摄影余量
            Integer photographyStock = session.getPhotographyStock();
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
}
