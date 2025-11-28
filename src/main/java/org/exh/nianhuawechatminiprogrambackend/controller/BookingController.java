package org.exh.nianhuawechatminiprogrambackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CheckSeatRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreateBookingRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CheckSeatResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreateBookingResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.SessionSeatDetailResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.ThemeDetailResponse;
import org.exh.nianhuawechatminiprogrambackend.service.BookingService;
import org.exh.nianhuawechatminiprogrambackend.service.CheckSeatService;
import org.exh.nianhuawechatminiprogrambackend.service.SessionSeatDetailService;
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

    @Autowired
    private SessionSeatDetailService sessionSeatDetailService;

    @Autowired
    private BookingService bookingService;

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

    @ApiOperation(value = "场次座位详情", notes = "获取场次的座位信息，包括哪些座位已被预订，哪些可选")
    @GetMapping("/enselectSeatDetail")
    public Result<SessionSeatDetailResponse> getSessionSeatDetail(
            @ApiParam(value = "场次类型 (lunch/dinner)", required = true, example = "lunch")
            @RequestParam(value = "sessionType", required = false) String sessionType,
            @ApiParam(value = "日期 (YYYY-MM-DD格式)", required = true, example = "2025-12-01")
            @RequestParam(value = "date", required = false) String date) {

        log.info("获取场次座位详情请求，sessionType={}, date={}", sessionType, date);

        SessionSeatDetailResponse response = sessionSeatDetailService.getSessionSeatDetail(sessionType, date);

        log.info("获取场次座位详情成功，座位总数：{}个", response.getSeatDetailList().size());
        return Result.success(response);
    }

    @ApiOperation(value = "创建预订", notes = "创建预订记录，锁定座位10分钟，返回预订ID、金额、支付状态和过期时间")
    @PostMapping("/createOrder")
    public Result<CreateBookingResponse> createBooking(@RequestBody @Valid CreateBookingRequest request) {
        log.info("创建预订请求，userId={}, sessionType={}, date={}, goodsCount={}, seatCount={}",
                request.getUserId(), request.getSessionType(), request.getDate(),
                request.getSelectedGoodList().size(), request.getSelectedSeatList().size());

        CreateBookingResponse response = bookingService.createBooking(request);

        log.info("创建预订成功，bookingId={}, amount={}", response.getBookingId(), response.getAmount());
        return Result.success(response);
    }
}
