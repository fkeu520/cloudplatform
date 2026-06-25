package com.cloudhub.platform.common;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * platform-common 是 library 模块, 没有主 Application 类。
 * <p>@SpringBootTest / @JsonTest 需要 @SpringBootConfiguration 才能加载上下文,
 * 此处提供一个空配置满足测试要求。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestApplication {
}
