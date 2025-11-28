package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.exh.nianhuawechatminiprogrambackend.entity.Booking;

import java.time.LocalDate;
import java.util.List;

/**
 * 预订Mapper
 */
@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    /**
     * 查询某日期用户的预订
     * @param userId 用户ID
     * @param date 日期
     * @return 预订列表
     */
    @Select("SELECT * FROM bookings " +
            "WHERE user_id = #{userId} " +
            "AND booking_date = #{date} " +
            "AND status NOT IN (2) " + // 2=已取消
            "AND is_deleted = 0 " +
            "ORDER BY created_at DESC")
    List<Booking> selectUserBookingsByDate(@Param("userId") Long userId,
                                        @Param("date") LocalDate date);

    /**
     * 查询用户今日未支付的预订数量
     * @param userId 用户ID
     * @param date 日期
     * @return 未支付预订数量
     */
    @Select("SELECT COUNT(*) FROM bookings " +
            "WHERE user_id = #{userId} " +
            "AND booking_date = #{date} " +
            "AND status = 0 " + // 0=待支付
            "AND is_deleted = 0")
    int countPendingPaymentsByUserAndDate(@Param("userId") Long userId,
                                         @Param("date") LocalDate date);
}
