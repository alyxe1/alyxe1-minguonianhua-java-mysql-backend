package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主题预订详情页响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThemeDetailResponse {

    /**
     * 头部图片
     */
    private String headerImage;

    /**
     * 主题标题
     */
    private String title;

    /**
     * 地址
     */
    private String address;
}
