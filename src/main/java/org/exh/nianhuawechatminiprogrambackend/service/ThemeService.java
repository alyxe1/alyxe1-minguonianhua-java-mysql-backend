package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.response.ThemeDetailResponse;

/**
 * 主题服务接口
 */
public interface ThemeService {

    /**
     * 获取主题预订详情页信息
     * @param themeType 主题类型（主题ID的字符串形式）
     * @return 主题详情
     */
    ThemeDetailResponse getThemeDetail(String themeType);
}
