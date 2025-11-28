package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 选择的商品DTO
 */
@Data
public class SelectedGood {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long goodId;

    /**
     * 选择的数量
     */
    @Min(value = 1, message = "数量至少为1")
    private Integer selectedCount;
}
