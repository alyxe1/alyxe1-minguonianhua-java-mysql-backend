package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付回调通知请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyRequest {

    /**
     * 通知ID
     */
    private String id;

    /**
     * 通知创建时间
     */
    private String createTime;

    /**
     * 通知类型
     */
    private String eventType;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源（包含加密数据）
     */
    private NotificationResource resource;

    /**
     * 回调摘要
     */
    private String summary;

    /**
     * 通知资源对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationResource {
        /**
         * 原始类型
         */
        private String originalType;

        /**
         * 加密算法
         */
        private String algorithm;

        /**
         * 密文（需要解密）
         */
        private String ciphertext;

        /**
         * 附加数据
         */
        private String associatedData;

        /**
         * 随机串
         */
        private String nonce;
    }
}
