package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.Session;

import java.time.LocalDate;
import java.util.List;

/**
 * 场次Mapper
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    /**
     * 根据主题ID和日期查询可用场次
     * @param themeId 主题ID
     * @param date 日期
     * @return 场次列表
     */
    @Select("SELECT * FROM sessions " +
            "WHERE theme_id = #{themeId} " +
            "AND status = 1 " +
            "AND is_deleted = 0 " +
            "AND DATE(start_time) = #{date} " +
            "ORDER BY start_time")
    List<Session> selectAvailableSessionsByDate(@Param("themeId") Long themeId,
                                                @Param("date") String date);

    /**
     * 根据主题ID和日期查询可用场次（使用MyBatis-Plus条件构造器）
     * @param themeId 主题ID
     * @param date 日期
     * @return 场次列表
     */
    default List<Session> selectAvailableSessions(Long themeId, String date) {
        return selectList(
                new LambdaQueryWrapper<Session>()
                        .eq(Session::getThemeId, themeId)
                        .eq(Session::getStatus, 1)
                        .eq(Session::getIsDeleted, 0)
                        .apply("DATE(start_time) = {0}", date)
                        .orderByAsc(Session::getStartTime)
        );
    }
}
