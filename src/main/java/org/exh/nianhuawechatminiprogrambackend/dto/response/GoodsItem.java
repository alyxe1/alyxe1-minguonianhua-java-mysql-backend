package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品列表项DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsItem {

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 商品类型
     */
    private String type;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 价格（元为单位，两位小数，例如：299.00）
     */
    private String price;

    /**
     * 标签
     */
    private String tag;

    /**
     * 商品ID
     */
    private Long goodId;
}
