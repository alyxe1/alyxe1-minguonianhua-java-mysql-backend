package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建预订时的选择商品 DTO
 */
@Data
public class SelectedGoodForBooking {

    /**
     * 商品ID（包含写真、化妆、摄影、座位商品包）
     */
    @NotBlank(message = "商品ID不能为空")
    private String goodId;

    /**
     * 选择数量
     */
    @NotNull(message = "选择数量不能为空")
    private Integer selectedCount;
}
