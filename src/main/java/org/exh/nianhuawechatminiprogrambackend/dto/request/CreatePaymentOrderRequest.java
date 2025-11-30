package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 创建支付订单请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentOrderRequest {

    /**
     * 预订ID（从创建预订接口获取）
     */
    @NotBlank(message = "预订ID不能为空")
    private String bookingId;

    /**
     * 用户的微信openid（小程序通过wx.login获取）
     */
    @NotBlank(message = "openid不能为空")
    private String openid;
}
