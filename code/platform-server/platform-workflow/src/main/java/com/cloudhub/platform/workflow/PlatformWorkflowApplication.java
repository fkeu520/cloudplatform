package com.cloudhub.platform.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.cloudhub.platform")
public class PlatformWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformWorkflowApplication.class, args);
    }
}
