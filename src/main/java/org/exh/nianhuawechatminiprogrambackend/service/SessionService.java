package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.response.AvailableSessionsResponse;

/**
 * 场次服务接口
 */
public interface SessionService {

    /**
     * 按日期获取可用场次
     * @param themeId 主题ID
     * @param date 日期（YYYY-MM-DD格式），如果为空则使用当前日期
     * @return 可用场次信息列表
     */
    AvailableSessionsResponse getAvailableSessions(Long themeId, String date);
}
