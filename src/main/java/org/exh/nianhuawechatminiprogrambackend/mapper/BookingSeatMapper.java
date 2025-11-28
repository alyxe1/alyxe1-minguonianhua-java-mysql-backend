package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.BookingSeat;

import java.time.LocalDate;
import java.util.List;

/**
 * 预订座位关联Mapper
 */
@Mapper
public interface BookingSeatMapper extends BaseMapper<BookingSeat> {

    /**
     * 查询某场次某天已预订的座位ID列表
     * @param sessionId 场次模板ID
     * @param date 日期
     * @return 已预订的座位ID列表
     * 注意：b.status != 2 表示排除已取消的预订（2=已取消）
     */
    @Select("SELECT DISTINCT bs.seat_id " +
            "FROM booking_seats bs " +
            "JOIN bookings b ON bs.booking_id = b.id " +
            "JOIN daily_sessions ds ON b.daily_session_id = ds.id " +
            "WHERE ds.session_id = #{sessionId} " +
            "AND ds.date = #{date} " +
            "AND b.status != 2")
    List<Long> selectBookedSeatIds(@Param("sessionId") Long sessionId,
                                   @Param("date") LocalDate date);
}
