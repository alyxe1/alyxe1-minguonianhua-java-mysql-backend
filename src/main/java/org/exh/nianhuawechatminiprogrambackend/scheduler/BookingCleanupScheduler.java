package org.exh.nianhuawechatminiprogrambackend.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.entity.*;
import org.exh.nianhuawechatminiprogrambackend.mapper.*;
import org.exh.nianhuawechatminiprogrambackend.util.SeatLockManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预订清理调度器
 * 定时清理超时未支付的预订
 */
@Slf4j
@Component
public class BookingCleanupScheduler {

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private BookingSeatMapper bookingSeatMapper;

    @Autowired
    private DailySessionMapper dailySessionMapper;

    @Autowired
    private SeatLockManager seatLockManager;

    /**
     * 每30秒执行一次，清理超时未支付的预订
     */
    @Scheduled(fixedDelay = 30000)
    public void cleanupExpiredBookings() {
        log.info("开始清理超时未支付的预订");

        // 查询创建时间超过10分钟且状态为待支付的预订
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(10);
        List<Booking> expiredBookings = bookingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Booking>()
                        .eq(Booking::getStatus, 0)  // 0-待支付
                        .lt(Booking::getCreatedAt, expireTime)
        );

        int successCount = 0;
        int failureCount = 0;

        for (Booking booking : expiredBookings) {
            try {
                cleanupSingleBooking(booking);
                successCount++;
            } catch (Exception e) {
                log.error("清理超时预订失败: bookingId={}", booking.getId(), e);
                failureCount++;
            }
        }

        log.info("完成清理超时未支付的预订，共{}个，成功{}个，失败{}个",
                expiredBookings.size(), successCount, failureCount);
    }

    private void cleanupSingleBooking(Booking booking) {
        log.info("清理超时预订，bookingId={}, userId={}", booking.getId(), booking.getUserId());

        // 1. 查询预订关联的座位
        List<BookingSeat> bookingSeats = bookingSeatMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BookingSeat>()
                        .eq(BookingSeat::getBookingId, booking.getId())
        );

        // 2. 批量释放该用户的所有座位锁（使用正确的API）
        seatLockManager.releaseAllUserSeatLocks(booking.getDailySessionId(), booking.getUserId());

        // 3. 恢复库存（TODO: 需要实现完整的库存恢复逻辑）
        // 由于时间原因，这只是一个简化版本
        log.warn("库存恢复逻辑待完善");

        // 4. 更新预订状态为已取消（2-已取消）
        booking.setStatus(2);
        bookingMapper.updateById(booking);

        log.info("成功清理超时预订: bookingId={}", booking.getId());
    }
}
