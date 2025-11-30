package org.exh.nianhuawechatminiprogrambackend.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付工具类
 * 用于生成签名和调用微信统一下单接口
 */
@Slf4j
public class WechatPayUtil {

    /**
     * 调用微信统一下单接口
     * @param appId 小程序AppID
     * @param mchid 商户号
     * @param merchantSerialNumber 商户证书序列号
     * @param privateKey 商户私钥
     * @param apiv3Key APIv3密钥
     * @param description 商品描述
     * @param outTradeNo 商户订单号
     * @param amount 金额（分）
     * @param openid 用户openid
     * @param notifyUrl 通知地址
     * @return prepay_id
     */
    public static String unifiedOrder(
            String appId,
            String mchid,
            String merchantSerialNumber,
            String privateKey,
            String apiv3Key,
            String description,
            String outTradeNo,
            Long amount,
            String openid,
            String notifyUrl) {

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("appid", appId);
            requestBody.put("mchid", mchid);
            requestBody.put("description", description);
            requestBody.put("out_trade_no", outTradeNo);

            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", amount);
            amountMap.put("currency", "CNY");
            requestBody.put("amount", amountMap);

            Map<String, Object> payerMap = new HashMap<>();
            payerMap.put("openid", openid);
            requestBody.put("payer", payerMap);

            requestBody.put("notify_url", notifyUrl);

            String jsonBody = JSONUtil.toJsonStr(requestBody);
            log.info("微信统一下单请求: {}", jsonBody);

            // TODO: 实现真实的微信统一下单API调用
            // 这里暂时返回模拟的prepay_id，实际项目中需要:
            // 1. 使用商户证书进行签名
            // 2. 调用微信统一下单接口: POST /v3/pay/transactions/jsapi
            // 3. 解析返回的prepay_id

            // 模拟返回prepay_id
            String prepayId = "wx" + IdUtil.fastSimpleUUID();
            log.info("微信统一下单响应 prepay_id: {}", prepayId);

            return prepayId;

        } catch (Exception e) {
            log.error("微信统一下单失败", e);
            throw new RuntimeException("微信统一下单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成前端调起支付所需签名
     * @param appId 小程序AppID
     * @param prepayId 预支付交易会话标识
     * @param privateKey 商户私钥
     * @return 签名字符串
     */
    public static String generatePaySign(String appId, String prepayId, String privateKey) {
        try {
            String timeStamp = String.valueOf(Instant.now().getEpochSecond());
            String nonceStr = RandomUtil.randomString(32);

            // 构建签名串
            String message = appId + "\n"
                    + timeStamp + "\n"
                    + nonceStr + "\n"
                    + "prepay_id=" + prepayId + "\n";

            // TODO: 使用商户私钥进行SHA256 with RSA签名
            // 这里暂时使用简单的SHA256哈希作为示例
            // 实际项目中需要使用商户私钥进行RSA签名

            log.info("待签名串: {}", message);

            // 模拟签名（实际应使用RSA签名）
            String paySign = SecureUtil.sha256(message).toUpperCase();

            return paySign;

        } catch (Exception e) {
            log.error("生成支付签名失败", e);
            throw new RuntimeException("生成支付签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成完整的支付参数
     * @param appId 小程序AppID
     * @param prepayId 预支付交易会话标识
     * @param privateKey 商户私钥
     * @param amount 支付金额（元）
     * @return 支付参数Map
     */
    public static Map<String, Object> buildPaymentParams(
            String appId,
            String prepayId,
            String privateKey,
            Double amount) {

        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = RandomUtil.randomString(32);
        String paySign = generatePaySign(appId, prepayId, privateKey);

        Map<String, Object> params = new HashMap<>();
        params.put("prepayId", prepayId);
        params.put("appId", appId);
        params.put("timeStamp", timeStamp);
        params.put("nonceStr", nonceStr);
        params.put("packageValue", "prepay_id=" + prepayId);
        params.put("signType", "RSA");
        params.put("paySign", paySign);
        params.put("amount", amount);

        return params;
    }
}
