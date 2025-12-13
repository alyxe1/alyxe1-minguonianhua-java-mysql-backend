package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 座位消耗配置DTO
 * 对应数据库goods表中seat_consumption_config字段
 * 示例值：[{"area": "front", "number": 2}, {"area": "middle", "number": 0}, {"area": "back", "number": 0}]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("座位消耗配置")
public class SeatConsumptionConfig {

    /**
     * 区域：front-前排，middle-中排，back-后排
     */
    @ApiModelProperty(value = "区域：front-前排，middle-中排，back-后排", required = true)
    private String area;

    /**
     * 该区域的座位数量
     */
    @ApiModelProperty(value = "该区域的座位数量", required = true)
    private Integer number;
}
