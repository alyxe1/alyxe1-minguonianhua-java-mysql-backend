package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预订实体类
 */
@Data
@TableName("bookings")
public class Booking {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("theme_id")
    private Long themeId;

    @TableField("daily_session_id")
    private Long dailySessionId;

    @TableField("order_no")
    private String orderNo;

    @TableField("total_amount")
    private Long totalAmount;

    @TableField("seat_count")
    private Integer seatCount;

    @TableField("booking_date")
    private LocalDate bookingDate;

    /**
     * 状态：0-待支付，1-已支付，2-已取消，3-已完成
     */
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
