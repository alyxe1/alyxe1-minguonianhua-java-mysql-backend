package org.exh.nianhuawechatminiprogrambackend.util;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.request.PaymentNotifyRequest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * 微信支付通知解密工具类
 * 使用AES-256-GCM算法解密微信回调通知
 */
@Slf4j
public class WechatPayDecryptUtil {

    private static final int TAG_LENGTH_BIT = 128;
    private static final int NONCE_LENGTH_BYTE = 12;

    /**
     * 解密微信支付通知
     * @param apiv3Key APIv3密钥
     * @param nonce 随机串
     * @param ciphertext 密文
     * @param associatedData 附加数据
     * @return 解密后的JSON字符串
     */
    public static String decrypt(String apiv3Key, String nonce, String ciphertext, String associatedData) {
        try {
            log.info("开始解密微信支付通知, nonce={}", nonce);

            // APIv3密钥
            byte[] key = apiv3Key.getBytes(StandardCharsets.UTF_8);

            // Base64解码密文
            byte[] encryptedData = Base64.getDecoder().decode(ciphertext);

            // 从encryptedData中提取nonce, tag和cipher
            byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);

            // 使用AES-256-GCM解密
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, nonceBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

            // 设置附加数据
            if (associatedData != null) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }

            // 执行解密
            byte[] decrypted = cipher.doFinal(encryptedData);

            String result = new String(decrypted, StandardCharsets.UTF_8);
            log.info("微信支付通知解密成功");

            return result;

        } catch (GeneralSecurityException e) {
            log.error("微信支付通知解密失败", e);
            throw new RuntimeException("微信支付通知解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密微信支付通知（从资源对象）
     * @param apiv3Key APIv3密钥
     * @param resource 通知资源对象
     * @return 解密后的JSON字符串
     */
    public static String decryptFromResource(String apiv3Key, PaymentNotifyRequest.NotificationResource resource) {
        return decrypt(
                apiv3Key,
                resource.getNonce(),
                resource.getCiphertext(),
                resource.getAssociatedData()
        );
    }

    /**
     * 解密并解析为对象
     * @param apiv3Key APIv3密钥
     * @param resource 通知资源对象
     * @param clazz 目标类
     * @param <T> 泛型类型
     * @return 解析后的对象
     */
    public static <T> T decryptAndParse(String apiv3Key, PaymentNotifyRequest.NotificationResource resource, Class<T> clazz) {
        String json = decryptFromResource(apiv3Key, resource);
        return JSONUtil.toBean(json, clazz);
    }
}
