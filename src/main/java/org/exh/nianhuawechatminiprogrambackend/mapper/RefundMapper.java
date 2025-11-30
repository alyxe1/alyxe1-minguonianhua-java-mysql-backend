package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.Refund;

/**
 * 退款Mapper
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    /**
     * 根据退款单号查询退款记录
     * @param refundNo 退款单号
     * @return 退款记录
     */
    @Select("SELECT * FROM refunds WHERE refund_no = #{refundNo}")
    Refund selectByRefundNo(@Param("refundNo") String refundNo);

    /**
     * 根据订单号查询退款记录
     * @param orderNo 订单号
     * @return 退款记录
     */
    @Select("SELECT * FROM refunds WHERE order_no = #{orderNo} ORDER BY id DESC LIMIT 1")
    Refund selectByOrderNo(@Param("orderNo") String orderNo);
}
