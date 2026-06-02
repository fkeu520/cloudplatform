package com.cloudhub.platform.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户中心启动类
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.cloudhub.platform")
public class PlatformUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformUserApplication.class, args);
    }
}