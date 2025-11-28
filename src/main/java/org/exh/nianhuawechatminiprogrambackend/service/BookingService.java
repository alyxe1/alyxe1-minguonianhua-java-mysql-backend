package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.request.CreateBookingRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreateBookingResponse;

/**
 * 预订服务接口
 */
public interface BookingService {

    /**
     * 创建预订
     * @param request 创建预订请求
     * @return 创建预订响应
     */
    CreateBookingResponse createBooking(CreateBookingRequest request);
}
