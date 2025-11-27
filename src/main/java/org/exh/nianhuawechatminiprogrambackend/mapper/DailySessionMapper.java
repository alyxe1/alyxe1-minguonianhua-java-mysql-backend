package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.DailySession;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日场次Mapper
 */
@Mapper
public interface DailySessionMapper extends BaseMapper<DailySession> {

    /**
     * 根据场次ID和日期查询每日场次
     * @param sessionId 场次模板ID
     * @param date 日期
     * @return 每日场次
     */
    @Select("SELECT * FROM daily_sessions " +
            "WHERE session_id = #{sessionId} " +
            "AND date = #{date}")
    DailySession selectBySessionIdAndDate(@Param("sessionId") Long sessionId,
                                          @Param("date") LocalDate date);

    /**
     * 批量查询某天的所有场次库存
     * @param sessionIds 场次ID列表
     * @param date 日期
     * @return 每日场次列表
     */
    @Select("<script>" +
            "SELECT * FROM daily_sessions " +
            "WHERE session_id IN " +
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND date = #{date}" +
            "</script>")
    List<DailySession> selectBySessionIdsAndDate(@Param("sessionIds") List<Long> sessionIds,
                                                 @Param("date") LocalDate date);
}
