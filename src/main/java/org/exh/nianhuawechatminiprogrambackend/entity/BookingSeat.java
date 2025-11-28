package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预订座位关联实体类
 */
@Data
@TableName("booking_seats")
public class BookingSeat {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("booking_id")
    private Long bookingId;

    @TableField("seat_id")
    private Long seatId;

    @TableField("seat_name")
    private String seatName;

    @TableField("price")
    private Long price;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
