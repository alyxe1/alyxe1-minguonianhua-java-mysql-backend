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
}
