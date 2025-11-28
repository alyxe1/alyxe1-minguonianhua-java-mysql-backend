package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CheckSeatRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CheckSeatResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.ThemeDetailResponse;
import org.exh.nianhuawechatminiprogrambackend.service.CheckSeatService;
import org.exh.nianhuawechatminiprogrambackend.service.ThemeService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 预订控制器
 */
@Api(tags = "预订模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    @Autowired
    private ThemeService themeService;

    @Autowired
    private CheckSeatService checkSeatService;

    @ApiOperation(value = "主题预订详情页", notes = "获取主题预订详情页信息，包含头部图片、标题和地址")
    @GetMapping("/themePageInfo")
    public Result<ThemeDetailResponse> getThemeDetail(
            @ApiParam(value = "主题类型（主题ID）", required = true, example = "1")
            @RequestParam("themeType") String themeType) {

        log.info("获取主题详情页请求，themeType={}", themeType);

        ThemeDetailResponse response = themeService.getThemeDetail(themeType);

        log.info("获取主题详情页成功，title={}", response.getTitle());
        return Result.success(response);
    }

    @ApiOperation(value = "校验座位是否可满足", notes = "根据用户选择的商品和数量，校验库存是否足够")
    @PostMapping("/checkSeats")
    public Result<CheckSeatResponse> checkSeats(@RequestBody @Valid CheckSeatRequest request) {
        log.info("校验座位请求，date={}, sessionType={}, goods={}",
                 request.getDate(), request.getSessionType(), request.getSelectedGoodList());

        CheckSeatResponse response = checkSeatService.checkSeats(request);

        log.info("校验座位成功，passed=true");
        return Result.success(response);
    }
}
