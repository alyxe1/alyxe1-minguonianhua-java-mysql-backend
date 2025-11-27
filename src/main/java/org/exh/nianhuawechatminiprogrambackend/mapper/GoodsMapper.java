package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.Goods;

import java.util.List;

/**
 * 商品Mapper
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 查询可用的商品列表
     * @param category 商品类别
     * @return 商品列表
     */
    @Select("SELECT * FROM goods " +
            "WHERE category = #{category} " +
            "AND status = 1 " +
            "AND is_deleted = 0")
    List<Goods> selectAvailableGoods(@Param("category") String category);

    /**
     * 根据类别查询可用商品（使用MyBatis-Plus条件构造器）
     * @param category 商品类别
     * @return 商品列表
     */
    default List<Goods> selectAvailableGoodsByCategory(String category) {
        return selectList(
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getCategory, category)
                        .eq(Goods::getStatus, 1)
                        .eq(Goods::getIsDeleted, 0)
        );
    }
}
