package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品查询响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsQueryResponse {

    /**
     * 商品列表
     */
    private List<GoodsItem> items;
}
