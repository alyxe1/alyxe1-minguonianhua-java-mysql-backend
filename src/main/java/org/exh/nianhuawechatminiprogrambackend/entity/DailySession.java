package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日场次实体类
 * 记录每天每个场次的实际库存
 */
@Data
@TableName("daily_sessions")
public class DailySession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("date")
    private LocalDate date;

    @TableField("available_seats")
    private Integer availableSeats;

    @TableField("makeup_stock")
    private Integer makeupStock;

    @TableField("photography_stock")
    private Integer photographyStock;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
