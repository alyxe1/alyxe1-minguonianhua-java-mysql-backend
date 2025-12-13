package org.exh.nianhuawechatminiprogrambackend.enums;

import lombok.Getter;

/**
 * 预订状态枚举
 * 接口文档定义: pending, paid, cancelled, refunded, completed
 * bookings表: 0-待支付，1-已支付，2-已取消，3-已完成
 */
@Getter
public enum BookingStatus {

    PENDING("pending", "待支付", 0),
    PAID("paid", "已支付", 1),
    CANCELLED("cancelled", "已取消", 2),
    COMPLETED("completed", "已完成", 3),
    REFUNDED("refunded", "已退款", 4); // bookings表中没有退款状态，但接口文档有，兼容orders表的已退款状态

    private final String code;
    private final String name;
    private final Integer dbValue;

    BookingStatus(String code, String name, Integer dbValue) {
        this.code = code;
        this.name = name;
        this.dbValue = dbValue;
    }

    /**
     * 根据code获取枚举
     */
    public static BookingStatus fromCode(String code) {
        for (BookingStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据dbValue获取枚举
     */
    public static BookingStatus fromDbValue(Integer dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (BookingStatus status : values()) {
            if (status.getDbValue().equals(dbValue)) {
                return status;
            }
        }
        return null;
    }
}
