package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.request.BookingDetailRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.CreateBookingRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.request.UserBookingListRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.BookingDetailResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.BookingListItemDTO;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CreateBookingResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.PageResult;

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

    /**
     * 获取用户预订列表
     * @param userId 用户ID
     * @param request 查询请求
     * @return 预订列表分页结果
     */
    PageResult<BookingListItemDTO> getUserBookingList(Long userId, UserBookingListRequest request);

    /**
     * 获取预订详情
     * @param request 预订详情请求
     * @return 预订详情响应
     */
    BookingDetailResponse getBookingDetail(BookingDetailRequest request);
}
