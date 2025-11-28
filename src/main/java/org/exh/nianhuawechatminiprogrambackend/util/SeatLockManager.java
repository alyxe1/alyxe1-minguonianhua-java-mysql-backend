package org.exh.nianhuawechatminiprogrambackend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 座位锁管理器
 * 使用 ConcurrentHashMap 确保高并发场景下每个座位只能被一个人抢到
 */
@Slf4j
@Component
public class SeatLockManager {

    /**
     * 座位锁存储：key = sessionId + "_" + seatId, value = 锁定信息
     */
    private final ConcurrentHashMap<String, SeatLock> seatLocks = new ConcurrentHashMap<>();

    /**
     * 锁定超时时间（分钟）
     */
    private static final long LOCK_TIMEOUT_MINUTES = 5;

    /**
     * 尝试锁定座位
     * @param sessionId 场次ID
     * @param seatId 座位ID
     * @param userId 用户ID
     * @return 是否锁定成功
     */
    public boolean tryLockSeat(Long sessionId, String seatId, Long userId) {
        String lockKey = buildLockKey(sessionId, seatId);

        SeatLock existingLock = seatLocks.get(lockKey);
        if (existingLock != null) {
            // 检查锁是否已过期
            if (isLockExpired(existingLock)) {
                log.info("座位锁已过期，自动释放：sessionId={}, seatId={}, lockTime={}",
                        sessionId, seatId, existingLock.getLockTime());
                seatLocks.remove(lockKey);
            } else {
                log.info("座位已被其他用户锁定：sessionId={}, seatId={}, lockedBy={}",
                        sessionId, seatId, existingLock.getUserId());
                return false;
            }
        }

        // 创建新的锁
        SeatLock newLock = new SeatLock(userId, LocalDateTime.now());
        seatLocks.put(lockKey, newLock);

        log.info("成功锁定座位：sessionId={}, seatId={}, userId={}", sessionId, seatId, userId);
        return true;
    }

    /**
     * 释放座位锁
     * @param sessionId 场次ID
     * @param seatId 座位ID
     * @param userId 用户ID
     */
    public void releaseSeatLock(Long sessionId, String seatId, Long userId) {
        String lockKey = buildLockKey(sessionId, seatId);
        SeatLock existingLock = seatLocks.get(lockKey);

        if (existingLock != null && existingLock.getUserId().equals(userId)) {
            seatLocks.remove(lockKey);
            log.info("成功释放座位锁：sessionId={}, seatId={}, userId={}", sessionId, seatId, userId);
        } else {
            log.warn("尝试释放非本人的座位锁：sessionId={}, seatId={}, userId={}", sessionId, seatId, userId);
        }
    }

    /**
     * 批量释放用户的座位锁
     * @param sessionId 场次ID
     * @param userId 用户ID
     */
    public void releaseAllUserSeatLocks(Long sessionId, Long userId) {
        String sessionPrefix = sessionId + "_";
        seatLocks.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(sessionPrefix) &&
                entry.getValue().getUserId().equals(userId)) {
                log.info("批量释放座位锁：seatId={}, userId={}",
                        entry.getKey().substring(sessionPrefix.length()), userId);
                return true;
            }
            return false;
        });
    }

    /**
     * 清理过期的锁
     */
    public void cleanExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;

        for (String lockKey : seatLocks.keySet()) {
            SeatLock lock = seatLocks.get(lockKey);
            if (lock != null && isLockExpired(lock)) {
                seatLocks.remove(lockKey);
                removedCount++;
                log.info("清理过期座位锁：seatId={}, lockTime={}",
                        lockKey.split("_")[1], lock.getLockTime());
            }
        }

        if (removedCount > 0) {
            log.info("清理过期座位锁完成，共清理{}个锁", removedCount);
        }
    }

    /**
     * 获取当前锁状态（用于监控）
     */
    public int getCurrentLockCount() {
        return seatLocks.size();
    }

    /**
     * 构建锁的key
     */
    private String buildLockKey(Long sessionId, String seatId) {
        return sessionId + "_" + seatId;
    }

    /**
     * 检查锁是否过期
     */
    private boolean isLockExpired(SeatLock lock) {
        return lock.getLockTime().plusMinutes(LOCK_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }

    /**
     * 座位锁信息
     */
    private static class SeatLock {
        private final Long userId;
        private final LocalDateTime lockTime;

        public SeatLock(Long userId, LocalDateTime lockTime) {
            this.userId = userId;
            this.lockTime = lockTime;
        }

        public Long getUserId() {
            return userId;
        }

        public LocalDateTime getLockTime() {
            return lockTime;
        }

        @Override
        public String toString() {
            return "SeatLock{userId=" + userId +
                   ", lockTime=" + lockTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "}";
        }
    }
}
