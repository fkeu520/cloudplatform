package com.cloudhub.platform.property;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cloudhub.platform")
public class ParkPropertyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParkPropertyApplication.class, args);
    }
}
