package com.cloudhub.platform.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置
 * <p>M5 P0-2 代码审查 (W6): 启用 @Async 支持，用于流程通知异步发送</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}