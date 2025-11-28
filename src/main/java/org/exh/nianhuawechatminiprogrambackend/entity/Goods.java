package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Data
@TableName("goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("sub_title")
    private String subTitle;

    @TableField("description")
    private String description;

    @TableField("price")
    private Long price;

    @TableField("image_url")
    private String imageUrl;

    @TableField("category")
    private String category; // photos-写真/摄影, makeup-化妆, seat_package-座位商品包, sets-套餐

    @TableField("status")
    private Integer status;

    @TableField("tag")
    private String tag; // 标签

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
