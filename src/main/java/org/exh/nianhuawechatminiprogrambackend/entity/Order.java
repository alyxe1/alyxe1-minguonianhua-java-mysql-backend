package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("booking_id")
    private Long bookingId;

    @TableField("order_no")
    private String orderNo;

    @TableField("total_amount")
    private Long totalAmount;

    @TableField("pay_amount")
    private Long payAmount;

    /**
     * 状态：0-待支付，1-已支付，2-支付失败，3-已退款
     */
    @TableField("status")
    private Integer status;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("payment_time")
    private LocalDateTime paymentTime;

    @TableField("transaction_id")
    private String transactionId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
