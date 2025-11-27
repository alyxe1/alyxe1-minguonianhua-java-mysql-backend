package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主题实体类
 */
@Data
@TableName("themes")
public class Theme {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("subtitle")
    private String subtitle;

    @TableField("description")
    private String description;

    @TableField("cover_image")
    private String coverImage;

    @TableField("banner_images")
    private String bannerImages; // JSON字符串

    @TableField("price")
    private Long price;

    @TableField("categories")
    private String categories; // JSON字符串

    @TableField("sold_count")
    private Integer soldCount;

    @TableField("status")
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
