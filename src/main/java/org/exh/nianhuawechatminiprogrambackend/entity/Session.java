package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次实体类
 */
@Data
@TableName("sessions")
public class Session {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("theme_id")
    private Long themeId;

    @TableField("session_type")
    private String sessionType;

    @TableField("session_name")
    private String sessionName;

    @TableField("max_capacity")
    private Integer maxCapacity;

    @TableField("available_seats")
    private Integer availableSeats;

    @TableField("makeup_stock")
    private Integer makeupStock;

    @TableField("photography_stock")
    private Integer photographyStock;

    @TableField("price")
    private Long price;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

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
