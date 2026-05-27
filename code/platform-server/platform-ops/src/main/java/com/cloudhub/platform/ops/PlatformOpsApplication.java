package com.cloudhub.platform.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.cloudhub.platform")
public class PlatformOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformOpsApplication.class, args);
    }
}
