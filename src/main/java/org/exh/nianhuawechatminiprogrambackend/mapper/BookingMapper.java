package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.exh.nianhuawechatminiprogrambackend.entity.Booking;

/**
 * 预订Mapper
 */
@Mapper
public interface BookingMapper extends BaseMapper<Booking> {
    // 可以添加自定义查询方法
}
