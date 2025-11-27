package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.exh.nianhuawechatminiprogrambackend.entity.Theme;

/**
 * 主题Mapper
 */
@Mapper
public interface ThemeMapper extends BaseMapper<Theme> {
    // BaseMapper提供基础CRUD方法
}
