package org.exh.nianhuawechatminiprogrambackend.service;

import org.exh.nianhuawechatminiprogrambackend.dto.response.VerificationConfirmResponse;
import org.exh.nianhuawechatminiprogrambackend.dto.response.VerificationVerifyResult;
import org.exh.nianhuawechatminiprogrambackend.entity.VerificationCode;

/**
 * 核销码服务接口
 */
public interface VerificationCodeService {

    /**
     * 根据订单号获取核销码
     * @param orderNo 订单号
     * @return 核销码信息
     */
    VerificationCode getVerificationCodeByOrderNo(String orderNo);

    /**
     * 核销验证（核对订单和预订信息）
     * @param code 核销码
     * @return 核销验证结果（包含订单核销信息）
     */
    VerificationVerifyResult verify(String code);

    /**
     * 核销确认（实际核销）
     * @param code 核销码
     * @param remarks 核销备注
     * @param adminId 管理员ID
     * @return 核销确认结果
     */
    VerificationConfirmResponse confirm(String code, String remarks, Long adminId);
}
