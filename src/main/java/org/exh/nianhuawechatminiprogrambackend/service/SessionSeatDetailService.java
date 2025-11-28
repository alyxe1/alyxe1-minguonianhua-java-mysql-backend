package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.response.SessionSeatDetailResponse;

/**
 * 场次座位详情服务接口
 */
public interface SessionSeatDetailService {

    /**
     * 获取场次座位详情
     * @param sessionType 场次类型 (lunch/dinner)
     * @param date 日期 (YYYY-MM-DD格式)
     * @return 场次座位详情响应
     */
    SessionSeatDetailResponse getSessionSeatDetail(String sessionType, String date);
}
