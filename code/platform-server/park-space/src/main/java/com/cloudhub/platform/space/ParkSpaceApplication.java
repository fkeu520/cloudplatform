package com.cloudhub.platform.space;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.cloudhub.platform")
public class ParkSpaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParkSpaceApplication.class, args);
    }
}
