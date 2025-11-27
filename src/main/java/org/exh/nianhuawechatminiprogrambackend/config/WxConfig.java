package org.exh.nianhuawechatminiprogrambackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wx")
public class WxConfig {
    private Miniapp miniapp = new Miniapp();
    private Pay pay = new Pay();

    @Data
    public static class Miniapp {
        private String appid;
        private String secret;
    }

    @Data
    public static class Pay {
        private String mchid;
        private String privateKeyPath;
        private String serialNumber;
        private String apiv3Key;
        private String notifyUrl;
    }
}
