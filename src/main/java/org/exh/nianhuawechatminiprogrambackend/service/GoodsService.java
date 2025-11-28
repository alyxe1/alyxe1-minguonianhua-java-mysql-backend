package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.response.GoodsQueryResponse;

/**
 * 商品服务接口
 */
public interface GoodsService {

    /**
     * 商品查询
     * @param type 商品类型(all-所有, photos-摄影服务, sets-套餐等)
     * @param pageNum 页码，从1开始
     * @param pageSize 每页大小
     * @return 商品列表
     */
    GoodsQueryResponse queryGoods(String type, Integer pageNum, Integer pageSize);
}
