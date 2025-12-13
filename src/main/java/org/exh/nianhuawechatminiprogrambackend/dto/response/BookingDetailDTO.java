package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("预订详情")
public class BookingDetailDTO {

    @ApiModelProperty(value = "过期时间（时间戳毫秒）", required = true)
    private Long expireTime;

    @ApiModelProperty(value = "订单ID", required = true)
    private String orderId;

    @ApiModelProperty(value = "日期", required = true)
    private String date;

    @ApiModelProperty(value = "支付状态", required = true)
    private String paymentStatus;

    @ApiModelProperty(value = "选中的座位列表", required = true)
    private List<SeatDetailDTO> selectedSeatList;

    @ApiModelProperty(value = "选中的商品列表", required = true)
    private List<SelectedGoodInfoDTO> selectedGoodList;

    @ApiModelProperty(value = "时间戳", required = true)
    private String timeStamp;

    @ApiModelProperty(value = "联系电话", required = true)
    private String contactPhone;

    @ApiModelProperty(value = "参考价格（元）- 并非来自订单，而来自对bookingId对应的所有选择商品的数量和价格的汇总求和", required = true)
    private String expectedPrice;
}
