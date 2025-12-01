package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.VerificationCode;

/**
 * 核销码Mapper
 */
@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {

    /**
     * 根据订单ID查询核销码
     * @param orderId 订单ID
     * @return 核销码信息
     */
    @Select("SELECT * FROM verification_codes WHERE order_id = #{orderId} ORDER BY id DESC LIMIT 1")
    VerificationCode selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据核销码查询
     * @param code 核销码
     * @return 核销码信息
     */
    @Select("SELECT * FROM verification_codes WHERE code = #{code}")
    VerificationCode selectByCode(@Param("code") String code);
}
