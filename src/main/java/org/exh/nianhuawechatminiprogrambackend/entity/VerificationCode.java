package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核销码实体类
 */
@Data
@TableName("verification_codes")
public class VerificationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("code")
    private String code;

    @TableField("qr_code_url")
    private String qrCodeUrl;

    /**
     * 状态：0-未使用，1-已使用，2-已过期
     */
    @TableField("status")
    private Integer status;

    @TableField("expiry_time")
    private LocalDateTime expiryTime;

    @TableField("verified_at")
    private LocalDateTime verifiedAt;

    @TableField("admin_id")
    private Long adminId;

    @TableField("remarks")
    private String remarks;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
