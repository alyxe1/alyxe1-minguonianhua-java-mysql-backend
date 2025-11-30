package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.Order;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} AND is_deleted = 0")
    Order selectByOrderNo(@Param("orderNo") String orderNo);
}
