package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.User;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据openid查询用户
     */
    @Select("SELECT * FROM users WHERE openid = #{openid} AND is_deleted = 0 LIMIT 1")
    User selectByOpenid(@Param("openid") String openid);
}
