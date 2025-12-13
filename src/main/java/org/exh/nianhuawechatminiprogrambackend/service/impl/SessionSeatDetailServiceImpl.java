package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.SeatDetail;
import org.exh.nianhuawechatminiprogrambackend.dto.response.SessionSeatDetailResponse;
import org.exh.nianhuawechatminiprogrambackend.entity.Seat;
import org.exh.nianhuawechatminiprogrambackend.entity.Session;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.BookingSeatMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SeatMapper;
import org.exh.nianhuawechatminiprogrambackend.mapper.SessionMapper;
import org.exh.nianhuawechatminiprogrambackend.service.SessionSeatDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 场次座位详情服务实现类
 */
@Slf4j
@Service
public class SessionSeatDetailServiceImpl implements SessionSeatDetailService {

    private static final String SEAT_TYPE_FRONT = "front";
    private static final String SEAT_TYPE_MIDDLE = "middle";
    private static final String SEAT_TYPE_BACK = "back";

    private static final String SEAT_TYPE_FRONT_NAME = "front";
    private static final String SEAT_TYPE_MIDDLE_NAME = "middle";
    private static final String SEAT_TYPE_BACK_NAME = "back";

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private BookingSeatMapper bookingSeatMapper;

    @Override
    public SessionSeatDetailResponse getSessionSeatDetail(String sessionType, String date) {
        log.info("获取场次座位详情，sessionType={}, date={}", sessionType, date);

        // 1. 参数校验
        if (sessionType == null || sessionType.isEmpty()) {
            log.error("场次类型不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "场次类型不能为空");
        }

        if (date == null || date.isEmpty()) {
            log.error("日期不能为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期不能为空");
        }

        // 2. 解析日期
        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(date);
        } catch (Exception e) {
            log.error("日期格式错误，应为YYYY-MM-DD格式: {}", date);
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期格式错误");
        }

        // 3. 查询场次模板
        Session session = getSessionByType(sessionType);
        if (session == null) {
            log.error("场次不存在，sessionType={}", sessionType);
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND, "场次不存在");
        }

        log.info("查询到场次模板，sessionId={}, sessionName={}", session.getId(), session.getSessionName());

        // 4. 查询该场次下的所有座位
        List<Seat> seats = getSeatsBySessionId(session.getId());
        if (seats.isEmpty()) {
            log.warn("该场次下没有配置座位，sessionId={}", session.getId());
            return buildEmptyResponse();
        }

        log.info("查询到座位总数：{}个", seats.size());

        // 5. 查询已预订的座位ID列表
        Set<Long> bookedSeatIds = new HashSet<>();
        try {
            bookedSeatIds = new HashSet<>(bookingSeatMapper.selectBookedSeatIds(session.getId(), bookingDate));
            log.info("查询到已预订座位数：{}个", bookedSeatIds.size());
        } catch (Exception e) {
            log.error("查询已预订座位失败", e);
            // 不抛出异常，继续执行，认为没有座位被预订
        }

        // 6. 构建座位详情列表
        List<SeatDetail> seatDetailList = new ArrayList<>();
        for (Seat seat : seats) {
            SeatDetail detail = new SeatDetail();
            detail.setSeatId(String.valueOf(seat.getId()));
            detail.setSeatName(seat.getSeatName());
            detail.setSeatType(getSeatTypeName(seat.getSeatType()));
            detail.setIsSelected(bookedSeatIds.contains(seat.getId()));

            seatDetailList.add(detail);
        }

        log.info("场次座位详情构建完成，总座位数：{}，已预订：{}",
                seatDetailList.size(), bookedSeatIds.size());

        // 7. 构建响应
        SessionSeatDetailResponse response = new SessionSeatDetailResponse();
        response.setSeatDetailList(seatDetailList);

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
     * 根据sessionId查询所有座位
     */
    private List<Seat> getSeatsBySessionId(Long sessionId) {
        return seatMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                        .eq(Seat::getSessionTemplateId, sessionId)
                        .eq(Seat::getStatus, 0)
        );
    }

    /**
     * 将seatType转换为中文名称
     */
    private String getSeatTypeName(String seatType) {
        if (seatType == null) {
            return "未知";
        }

        switch (seatType) {
            case SEAT_TYPE_FRONT:
                return SEAT_TYPE_FRONT_NAME;
            case SEAT_TYPE_MIDDLE:
                return SEAT_TYPE_MIDDLE_NAME;
            case SEAT_TYPE_BACK:
                return SEAT_TYPE_BACK_NAME;
            default:
                return seatType;
        }
    }

    /**
     * 构建空响应（当没有座位时）
     */
    private SessionSeatDetailResponse buildEmptyResponse() {
        SessionSeatDetailResponse response = new SessionSeatDetailResponse();
        response.setSeatDetailList(new ArrayList<>());
        return response;
    }
}
