package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.request.CheckSeatRequest;
import org.exh.nianhuawechatminiprogrambackend.dto.response.CheckSeatResponse;

/**
 * 座位校验服务接口
 */
public interface CheckSeatService {

    /**
     * 校验座位是否可满足
     * @param request 校验请求
     * @return 校验响应（包含原请求的商品列表）
     */
    CheckSeatResponse checkSeats(CheckSeatRequest request);
}
