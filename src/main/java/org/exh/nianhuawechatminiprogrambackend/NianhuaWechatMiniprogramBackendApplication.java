package org.exh.nianhuawechatminiprogrambackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NianhuaWechatMiniprogramBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NianhuaWechatMiniprogramBackendApplication.class, args);
    }

}
