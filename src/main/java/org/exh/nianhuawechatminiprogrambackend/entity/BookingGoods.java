package org.exh.nianhuawechatminiprogrambackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预订商品关联实体类
 */
@Data
@TableName("booking_goods")
public class BookingGoods {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("booking_id")
    private Long bookingId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("price")
    private Long price;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
