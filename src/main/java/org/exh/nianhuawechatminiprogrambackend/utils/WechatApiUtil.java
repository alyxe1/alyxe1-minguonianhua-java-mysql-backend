package org.exh.nianhuawechatminiprogrambackend.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.config.WxConfig;
import org.exh.nianhuawechatminiprogrambackend.dto.response.WechatSessionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 微信API工具类
 */
@Slf4j
@Component
public class WechatApiUtil {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WxConfig wxConfig;

    /**
     * 调用微信code2session接口
     *
     * @param code 小程序登录code
     * @return 微信session响应
     */
    public WechatSessionResponse code2Session(String code) {
        try {
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    CODE2SESSION_URL,
                    wxConfig.getMiniapp().getAppid(),
                    wxConfig.getMiniapp().getSecret(),
                    code);

            log.info("调用微信code2session接口, url: {}", url);
            String result = HttpUtil.get(url);
            log.info("微信code2session响应: {}", result);

            JSONObject jsonObject = JSONUtil.parseObj(result);
            WechatSessionResponse response = new WechatSessionResponse();
            response.setOpenid(jsonObject.getStr("openid"));
            response.setSessionKey(jsonObject.getStr("session_key"));
            response.setUnionid(jsonObject.getStr("unionid"));
            response.setErrcode(jsonObject.getInt("errcode"));
            response.setErrmsg(jsonObject.getStr("errmsg"));

            return response;
        } catch (Exception e) {
            log.error("调用微信code2session接口失败", e);
            throw new RuntimeException("调用微信接口失败", e);
        }
    }
}
