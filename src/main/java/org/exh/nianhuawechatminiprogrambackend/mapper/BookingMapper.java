package org.exh.nianhuawechatminiprogrambackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
     * 查询用户在某日期的预订数量
     * @param userId 用户ID
     * @param date 日期
     * @param statuses 预订状态列表
     * @return 预订数量
     */
    @Select("SELECT COUNT(*) FROM bookings " +
            "WHERE user_id = #{userId} " +
            "AND booking_date = #{date} " +
            "AND status IN " +
            "<foreach collection='statuses' item='status' open='(' separator=',' close=')'>" +
            "#{status}" +
            "</foreach> " +
            "AND is_deleted = 0")
    int countUserBookingsByDateAndStatus(@Param("userId") Long userId,
                                          @Param("date") LocalDate date,
                                          @Param("statuses") List<Integer> statuses);

    /**
     * 检查座位是否已被预订（在指定日期和场次中）
     * @param sessionTemplateId 场次模板ID
     * @param date 日期
     * @param seatIds 座位ID列表
     * @return 已预订的座位数量
     */
    @Select("SELECT COUNT(*) FROM booking_seats bs " +
            "JOIN bookings b ON bs.booking_id = b.id " +
            "JOIN daily_sessions ds ON b.daily_session_id = ds.id " +
            "WHERE ds.session_id = #{sessionTemplateId} " +
            "AND ds.date = #{date} " +
            "AND bs.seat_id IN " +
            "<foreach collection='seatIds' item='seatId' open='(' separator=',' close=')'>" +
            "#{seatId}" +
            "</foreach> " +
            "AND b.status != 2 " +
            "AND b.is_deleted = 0")
    int countBookedSeats(@Param("sessionTemplateId") Long sessionTemplateId,
                         @Param("date") LocalDate date,
                         @Param("seatIds") List<String> seatIds);

    /**
     * 根据订单号查询预订
     * @param orderNo 订单号
     * @return 预订信息
     */
    @Select("SELECT * FROM bookings " +
            "WHERE order_no = #{orderNo} " +
            "AND is_deleted = 0")
    Booking selectByOrderNo(@Param("orderNo") String orderNo);
}
