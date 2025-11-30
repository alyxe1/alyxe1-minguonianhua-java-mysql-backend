package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.Data;

/**
 * 微信支付通知解密后数据
 * 对应微信回调通知解密后的JSON结构
 */
@Data
public class WechatPaymentNotification {

    /**
     * 微信支付交易号
     */
    private String transactionId;

    /**
     * 金额信息
     */
    private Amount amount;

    /**
     * 商户订单号（我们系统的订单号）
     */
    private String outTradeNo;

    /**
     * 支付成功时间
     */
    private String successTime;

    /**
     * 交易状态
     */
    private String tradeState;

    /**
     * 金额信息内部类
     */
    @Data
    public static class Amount {
        /**
         * 总金额（分）
         */
        private Long total;

        /**
         * 用户支付金额（分）
         */
        private Long payerTotal;

        /**
         * 货币类型
         */
        private String currency;

        /**
         * 用户支付货币类型
         */
        private String payerCurrency;
    }
}
