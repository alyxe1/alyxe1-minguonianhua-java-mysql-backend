package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.GoodsQueryResponse;
import org.exh.nianhuawechatminiprogrambackend.service.GoodsService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器
 */
@Api(tags = "首页模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/homepage")
public class HomepageController {

    @Autowired
    private GoodsService goodsService;

    @ApiOperation(value = "商品查询", notes = "根据类型查询商品列表，支持分页")
    @GetMapping("/query")
    public Result<GoodsQueryResponse> queryGoods(
            @ApiParam(value = "商品类型(all-所有, photos-摄影服务, sets-套餐等)", required = false, example = "all")
            @RequestParam(value = "type", required = false) String type,

            @ApiParam(value = "页码，从1开始", required = false, example = "1")
            @RequestParam(value = "pageNum", required = false) Integer pageNum,

            @ApiParam(value = "每页大小", required = false, example = "10")
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {

        log.info("商品查询请求，type={}, pageNum={}, pageSize={}", type, pageNum, pageSize);

        GoodsQueryResponse response = goodsService.queryGoods(type, pageNum, pageSize);

        log.info("商品查询成功，共{}条记录", response.getItems().size());
        return Result.success(response);
    }
}
