package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.exh.nianhuawechatminiprogrambackend.entity.BookingGoods;

/**
 * 预订商品关联Mapper
 */
@Mapper
public interface BookingGoodsMapper extends BaseMapper<BookingGoods> {

    /**
     * 批量插入（MyBatis-Plus提供的批量插入方法）
     *
     * @param entityList 实体列表
     * @return 插入成功的数量
     */
    int insertBatchSomeColumn(java.util.List<BookingGoods> entityList);
}
