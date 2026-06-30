package com.cloudhub.platform.enterprise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 企业档案服务启动类 (park-enterprise)
 *
 * <p>csyh 业务融合 Phase 1: 企业档案核心 CRUD.
 * <p>端口 8094,与 park-space(8091) / park-property(8092) / park-contract(8093) 同属 park-* 微服务集群.
 * <p>包扫描: com.cloudhub.platform.* 全局,沿用 platform-common 自动装配.
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.cloudhub.platform")
public class ParkEnterpriseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParkEnterpriseApplication.class, args);
    }
}
