package org.exh.nianhuawechatminiprogrambackend.enums;

import lombok.Getter;

/**
 * 统一返回结果状态码枚举
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(0, "操作成功"),

    // 客户端错误
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "请求冲突"),

    // 服务端错误
    ERROR(500, "服务器内部错误"),
    NOT_IMPLEMENTED(501, "功能未实现"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务错误
    USER_NOT_LOGIN(1001, "用户未登录"),
    TOKEN_INVALID(1002, "Token无效"),
    TOKEN_EXPIRED(1003, "Token已过期"),
    USER_NOT_FOUND(1004, "用户不存在"),
    THEME_NOT_FOUND(1005, "主题不存在"),
    SESSION_NOT_FOUND(1006, "场次不存在"),
    SEAT_NOT_AVAILABLE(1007, "座位不可用"),
    BOOKING_CONFLICT(1008, "预订冲突"),
    ORDER_NOT_FOUND(1009, "订单不存在"),
    ORDER_CANNOT_CANCEL(1010, "订单无法取消"),
    ORDER_CANNOT_REFUND(1011, "订单无法退款"),
    PAYMENT_FAILED(1012, "支付失败"),
    PAYMENT_STATUS_ERROR(1013, "支付状态错误"),
    VERIFICATION_CODE_INVALID(1014, "核销码无效"),
    VERIFICATION_CODE_EXPIRED(1015, "核销码已过期"),
    SEAT_ALREADY_LOCKED(1016, "座位已被锁定"),
    INVENTORY_INSUFFICIENT(1017, "库存不足"),

    // 其他
    PARSE_ERROR(2001, "解析错误"),
    NETWORK_ERROR(2002, "网络错误"),
    UPLOAD_FAILED(2003, "上传失败"),
    FILE_NOT_FOUND(2004, "文件不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
