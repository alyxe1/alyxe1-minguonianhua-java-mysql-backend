package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场次实体类
 * 场次模板，不绑定具体日期
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

    @TableField("total_seats")
    private Integer totalSeats;

    @TableField("total_makeup")
    private Integer totalMakeup;

    @TableField("total_photography")
    private Integer totalPhotography;

    @TableField("price")
    private Long price;

    @TableField("start_time")
    private LocalTime startTime;

    @TableField("end_time")
    private LocalTime endTime;

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
