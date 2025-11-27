package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.AvailableSessionsResponse;
import org.exh.nianhuawechatminiprogrambackend.service.SessionService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 场次控制器
 */
@Api(tags = "场次模块")
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @ApiOperation(value = "获取可用场次", notes = "按日期获取主题的可用场次信息，包含座位、化妆、摄影的余量")
    @GetMapping("/sessions/available")
    public Result<AvailableSessionsResponse> getAvailableSessions(
            @ApiParam(value = "主题ID", required = true, example = "1")
            @RequestParam("themeType") Long themeType,
            @ApiParam(value = "日期(YYYY-MM-DD格式)", required = false, example = "2025-11-28")
            @RequestParam(value = "date", required = false) String date) {

        log.info("获取可用场次请求，themeType={}, date={}", themeType, date);

        AvailableSessionsResponse response = sessionService.getAvailableSessions(themeType, date);

        log.info("获取可用场次成功，共{}个场次", response.getItems().size());
        return Result.success(response);
    }
}
