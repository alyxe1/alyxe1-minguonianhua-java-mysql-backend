package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("预订列表项")
public class BookingListItemDTO {

    @ApiModelProperty("预订ID")
    private Long id;

    @ApiModelProperty("预订号")
    private String bookingId;

    @ApiModelProperty("金额")
    private BigDecimal amount;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("状态名称")
    private String statusName;

    @ApiModelProperty("支付方式")
    private String paymentMethod;

    @ApiModelProperty("支付时间")
    private LocalDateTime paymentTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("预订信息")
    private BookingInfoDTO bookingInfo;
}
