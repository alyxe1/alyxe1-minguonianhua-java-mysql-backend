package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品列表项DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("商品列表项")
public class GoodsItem {

    /**
     * 图片URL
     */
    @ApiModelProperty(value = "图片URL", required = true)
    private String imageUrl;

    /**
     * 商品类型
     */
    @ApiModelProperty(value = "商品类型", required = true)
    private String type;

    /**
     * 商品标题
     */
    @ApiModelProperty(value = "商品标题", required = true)
    private String title;

    /**
     * 商品副标题
     */
    @ApiModelProperty(value = "商品副标题")
    private String subTitle;

    /**
     * 商品描述
     */
    @ApiModelProperty(value = "商品描述")
    private String description;

    /**
     * 价格（元为单位，两位小数，例如：299.00）
     */
    @ApiModelProperty(value = "价格（元）", required = true)
    private String price;

    /**
     * 标签
     */
    @ApiModelProperty(value = "标签")
    private String tag;

    /**
     * 商品ID
     */
    @ApiModelProperty(value = "商品ID", required = true)
    private Long goodId;

    /**
     * 座位消耗配置，对应数据库goods表中seat_consumption_config字段
     * 类型为json，示例值为：[{"area": "front", "number": 2}, {"area": "middle", "number": 0}, {"area": "back", "number": 0}]
     */
    @ApiModelProperty(value = "座位消耗配置，对应数据库goods表中seat_consumption_config字段", required = true)
    private List<SeatConsumptionConfig> seatConsumptionConfig;
}
