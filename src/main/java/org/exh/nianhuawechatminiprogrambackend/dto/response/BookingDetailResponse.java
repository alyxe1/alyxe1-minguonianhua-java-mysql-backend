package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("预订详情响应")
public class BookingDetailResponse {

    @ApiModelProperty(value = "预订详情", required = true)
    private BookingDetailDTO bookingDetail;

    @ApiModelProperty(value = "主题图片URL", required = true)
    private String themePic;

    @ApiModelProperty(value = "主题名称", required = true)
    private String themeName;

    @ApiModelProperty(value = "场次类型", required = true, notes = "lunch/dinner")
    private String sessionType;

    @ApiModelProperty(value = "地址", required = true)
    private String address;
}
