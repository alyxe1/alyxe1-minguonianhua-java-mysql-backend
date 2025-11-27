package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.exh.nianhuawechatminiprogrambackend.entity.Seat;

import java.util.List;

/**
 * 座位Mapper
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 批量插入（MyBatis-Plus提供的批量插入方法）
     *
     * @param entityList 实体列表
     * @return 插入成功的数量
     */
    int insertBatchSomeColumn(List<Seat> entityList);
}
