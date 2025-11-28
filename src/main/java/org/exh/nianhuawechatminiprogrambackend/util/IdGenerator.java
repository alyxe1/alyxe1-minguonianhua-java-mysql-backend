package org.exh.nianhuawechatminiprogrambackend.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器工具类
 */
public class IdGenerator {

    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis());

    /**
     * 生成订单号（使用时间戳+自增序列）
     * @return 唯一的订单号
     */
    public static String generateOrderNo() {
        // 使用时间戳 + 自增序列，确保唯一性
        long timestamp = System.currentTimeMillis();
        long sequence = counter.incrementAndGet() % 10000;
        return String.format("%d%04d", timestamp, sequence);
    }

    /**
     * 生成UUID格式的订单号
     * @return UUID订单号
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * 生成预订ID（使用时间戳+随机数）
     * @return 唯一的预订ID
     */
    public static String generateBookingId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return String.format("BK%d%03d", timestamp, random);
    }
}