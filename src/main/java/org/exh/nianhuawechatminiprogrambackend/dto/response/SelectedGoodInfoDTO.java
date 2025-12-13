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
@ApiModel("选中商品信息")
public class SelectedGoodInfoDTO {

    @ApiModelProperty(value = "商品ID", required = true)
    private String goodId;

    @ApiModelProperty(value = "选择数量", required = true)
    private Integer selectedCount;

    @ApiModelProperty(value = "商品名称（对应good表中的name字段）", required = true)
    private String goodName;

    @ApiModelProperty(value = "商品价格（对应good表中的price字段，已转换为元，保留两位小数）", required = true)
    private String goodPrice;
}
