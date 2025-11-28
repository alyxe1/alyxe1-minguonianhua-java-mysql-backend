package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位实体类
 */
@Data
@TableName("seats")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_template_id")
    private Long sessionTemplateId;

    @TableField("seat_id")
    private String seatId;

    @TableField("seat_name")
    private String seatName;

    @TableField("seat_type")
    private String seatType;

    @TableField("status")
    private Integer status;

    @TableField("price")
    private Long price;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
