package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.exh.nianhuawechatminiprogrambackend.entity.BookingGoods;

/**
 * 预订商品关联Mapper
 */
@Mapper
public interface BookingGoodsMapper extends BaseMapper<BookingGoods> {
}
