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
@ApiModel("座位详情")
public class SeatDetailDTO {

    @ApiModelProperty(value = "座位ID", required = true)
    private String seatId;

    @ApiModelProperty(value = "座位名称", required = true)
    private String seatName;
}
