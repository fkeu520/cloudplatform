package com.cloudhub.platform.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置
 * <p>M5 P0-2 代码审查: 启用 @Async 支持，用于操作日志异步写入</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
