package com.cloudhub.platform.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RateLimitFilter Lua 脚本单测
 * <p>验证 Lua 脚本:
 * <ol>
 *   <li>存在且非空</li>
 *   <li>包含关键 Redis 调用 (ZREMRANGEBYSCORE, ZCARD, ZADD, PEXPIRE)</li>
 *   <li>返回 1/0 协议正确</li>
 * </ol>
 * </p>
 * <p>完整滑动窗口逻辑验证需要 Testcontainers + Redis (留给集成测试).</p>
 */
class RateLimitFilterTest {

    @Test
    void rateLimitScript_exists() {
        ClassPathResource resource = new ClassPathResource("scripts/rate_limit.lua");
        assertTrue(resource.exists(), "rate_limit.lua 必须存在 classpath:scripts/");
    }

    @Test
    void rateLimitScript_containsKeyRedisOps() throws Exception {
        ClassPathResource resource = new ClassPathResource("scripts/rate_limit.lua");
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }
        assertNotNull(content);
        assertTrue(content.contains("ZREMRANGEBYSCORE"), "必须清理窗口外请求");
        assertTrue(content.contains("ZCARD"), "必须统计当前窗口请求数");
        assertTrue(content.contains("ZADD"), "必须记录本次请求");
        assertTrue(content.contains("PEXPIRE"), "必须设置 key 过期防内存泄漏");
    }

    @Test
    void rateLimitScript_returnsAllowedAndRejected() throws Exception {
        ClassPathResource resource = new ClassPathResource("scripts/rate_limit.lua");
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }
        assertTrue(content.contains("return 1"), "允许时返回 1");
        assertTrue(content.contains("return 0"), "拒绝时返回 0");
    }
}
