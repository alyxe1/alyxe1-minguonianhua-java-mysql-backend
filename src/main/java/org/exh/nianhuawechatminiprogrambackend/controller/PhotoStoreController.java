package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.GoodsQueryResponse;
import org.exh.nianhuawechatminiprogrambackend.service.GoodsService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 写真商店控制器
 * 写真列表接口是商品查询接口的特例（type=photos）
 */
@Api(tags = "写真商店模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/homepage")
public class PhotoStoreController {

    @Autowired
    private GoodsService goodsService;

    @ApiOperation(value = "写真列表", notes = "获取所有写真商品列表，是商品查询接口的特例（type=photos）")
    @GetMapping("/photoStore")
    public Result<GoodsQueryResponse> getPhotoStore() {
        log.info("获取写真列表请求");

        // 固定type为photos，查询所有写真商品，不分页
        GoodsQueryResponse response = goodsService.queryGoods("photos", null, null);

        log.info("获取写真列表成功，共{}条记录", response.getItems().size());
        return Result.success(response);
    }
}
