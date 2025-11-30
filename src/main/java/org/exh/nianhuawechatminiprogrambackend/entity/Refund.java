package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退款记录实体类
 */
@Data
@TableName("refunds")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("refund_no")
    private String refundNo;

    @TableField("transaction_id")
    private String transactionId;

    @TableField("refund_id")
    private String refundId;

    @TableField("total_amount")
    private Long totalAmount;

    @TableField("refund_amount")
    private Long refundAmount;

    /**
     * 退款状态：0-退款中，1-退款成功，2-退款失败，3-退款关闭
     */
    @TableField("status")
    private Integer status;

    @TableField("reason")
    private String reason;

    @TableField("notify_result")
    private String notifyResult;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
