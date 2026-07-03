package com.cloudhub.platform.auth.service;

import com.cloudhub.platform.auth.domain.StepUpToken;
import com.cloudhub.platform.auth.domain.mapper.StepUpTokenMapper;
import com.cloudhub.platform.common.exception.BizException;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-3 Step-up Token 服务单元测试 (8 TC)
 * <p>Day 1: 纯 Mockito 单元测试, 不依赖 Spring 容器, 跑得快.</p>
 * <p>Day 5 补 @SpringBootTest 集成测试 (TC-04/05/06/07/08).</p>
 *
 * <h2>测试策略</h2>
 * <ul>
 *   <li>Mock StepUpTokenMapper + StringRedisTemplate + RestTemplate</li>
 *   <li>用 ReflectionTestUtils 注入 @Value 字段</li>
 *   <li>每个测试独立, 不依赖 Spring 上下文</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0-3 Step-up Token Service 单元测试 (8 TC)")
class StepUpTokenServiceTest {

    @Mock
    private StepUpTokenMapper mapper;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private StepUpTokenService service;

    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_SCOPE = "tenant:delete";
    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_UA = "Mozilla/5.0 Test";

    @BeforeEach
    void setUp() {
        // 注入 @Value 字段 (绕过 Spring 容器)
        ReflectionTestUtils.setField(service, "ttlSeconds", 300L);
        ReflectionTestUtils.setField(service, "maxIssuesPerWindow", 3);
        ReflectionTestUtils.setField(service, "rateLimitSeconds", 300L);
        ReflectionTestUtils.setField(service, "userServiceUrl", "http://test-user:8081");
    }

    /**
     * 模拟密码验证成功 — 返回 user service 标准响应
     */
    private void mockPasswordVerifySuccess() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("data", Map.of("id", TEST_USER_ID));
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(resp);
    }

    @Test
    @Order(1)
    @DisplayName("TC-01: 正常签发 — 返回 32 字节 token, expiresAt = now+5min")
    void tc01_issue_normal() {
        // given: 频率限制 + 密码验证 OK
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        mockPasswordVerifySuccess();
        when(mapper.insert(any(StepUpToken.class))).thenAnswer(inv -> {
            StepUpToken arg = inv.getArgument(0);
            arg.setId(1L);
            return 1;
        });

        // when
        StepUpTokenService.IssueResult result = service.issue(
                TEST_USER_ID, 1L, TEST_SCOPE, true, "plainPwd", TEST_IP, TEST_UA);

        // then
        assertNotNull(result.token(), "token 不应为空");
        assertTrue(result.token().length() >= 40, "Base64 32字节 token 长度应 >= 40");
        assertNotNull(result.expiresAt(), "expiresAt 不应为空");
        assertTrue(result.expiresAt().isAfter(LocalDateTime.now().plusSeconds(290)),
                "expiresAt 应在 5 分钟内");
        assertTrue(result.singleUse());
        assertEquals(TEST_SCOPE, result.scope());

        // 验证 mapper.insert 被调用
        ArgumentCaptor<StepUpToken> captor = ArgumentCaptor.forClass(StepUpToken.class);
        verify(mapper).insert(captor.capture());
        StepUpToken saved = captor.getValue();
        assertEquals(TEST_USER_ID, saved.getUserId());
        assertNotNull(saved.getTokenHash(), "应存哈希不存明文");
        assertNotEquals(result.token(), saved.getTokenHash(), "哈希与明文应不同");
    }

    @Test
    @Order(2)
    @DisplayName("TC-02: 5 分钟内连续 4 次申请 — 第 4 次抛频繁异常 (rate limit)")
    void tc02_issue_rateLimit() {
        // given: 频率计数已达 3
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("3");

        // when + then
        BizException ex = assertThrows(BizException.class, () ->
                service.issue(TEST_USER_ID, 1L, TEST_SCOPE, true, "plainPwd", TEST_IP, TEST_UA));
        assertTrue(ex.getMessage().contains("频繁"));

        // 验证未走到密码验证
        verify(restTemplate, never()).postForObject(anyString(), any(), eq(Map.class));
        verify(mapper, never()).insert(any(StepUpToken.class));
    }

    @Test
    @Order(3)
    @DisplayName("TC-03: 密码错误 — 抛 BizException (强鉴权拦截)")
    void tc03_issue_wrongPassword() {
        // given: 频率 OK, 但密码错误 (user service 返回 401)
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 401);
        resp.put("message", "密码错误");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(resp);

        // when + then
        BizException ex = assertThrows(BizException.class, () ->
                service.issue(TEST_USER_ID, 1L, TEST_SCOPE, true, "plainPwd", TEST_IP, TEST_UA));
        assertTrue(ex.getMessage().contains("密码") || ex.getMessage().contains("失败"));

        // 验证未落库
        verify(mapper, never()).insert(any(StepUpToken.class));
    }

    @Test
    @Order(4)
    @DisplayName("TC-04: 验证并消费 — 单次有效, used 字段被置 true")
    void tc04_verify_singleUse_consumes() {
        // given: 存在一个未使用的单次 token
        String token = "valid-token-123";
        String tokenHash = DigestUtils.sha256Hex(token);
        StepUpToken stored = new StepUpToken();
        stored.setId(42L);
        stored.setTokenHash(tokenHash);
        stored.setUserId(TEST_USER_ID);
        stored.setScope(TEST_SCOPE);
        stored.setSingleUse(true);
        stored.setUsed(false);
        stored.setExpiresAt(LocalDateTime.now().plusSeconds(300));
        stored.setRevoked(false);
        when(mapper.selectByHash(tokenHash)).thenReturn(stored);
        when(mapper.updateById(any(StepUpToken.class))).thenReturn(1);

        // when
        Long tokenId = service.verifyAndConsume(token, TEST_SCOPE, TEST_IP, TEST_UA, false);

        // then
        assertEquals(42L, tokenId);
        ArgumentCaptor<StepUpToken> captor = ArgumentCaptor.forClass(StepUpToken.class);
        verify(mapper).updateById(captor.capture());
        assertTrue(captor.getValue().getUsed(), "used 字段应被置 true");
        assertNotNull(captor.getValue().getConsumedAt());
    }

    @Test
    @Order(5)
    @DisplayName("TC-05: 验证后再次使用 — 抛 '已使用' 异常 (防重放)")
    void tc05_verify_replay_blocked() {
        // given: 已使用的 token
        String token = "used-token-123";
        String tokenHash = DigestUtils.sha256Hex(token);
        StepUpToken stored = new StepUpToken();
        stored.setId(43L);
        stored.setTokenHash(tokenHash);
        stored.setUserId(TEST_USER_ID);
        stored.setScope(TEST_SCOPE);
        stored.setSingleUse(true);
        stored.setUsed(true); // 已使用
        stored.setExpiresAt(LocalDateTime.now().plusSeconds(300));
        stored.setRevoked(false);
        when(mapper.selectByHash(tokenHash)).thenReturn(stored);

        // when + then
        BizException ex = assertThrows(BizException.class, () ->
                service.verifyAndConsume(token, TEST_SCOPE, TEST_IP, TEST_UA, false));
        assertTrue(ex.getMessage().contains("已使用"));
    }

    @Test
    @Order(6)
    @DisplayName("TC-06: 过期 token — 抛 '已过期' 异常")
    void tc06_verify_expired() {
        // given: 过期 token
        String token = "expired-token-123";
        String tokenHash = DigestUtils.sha256Hex(token);
        StepUpToken stored = new StepUpToken();
        stored.setId(44L);
        stored.setTokenHash(tokenHash);
        stored.setUserId(TEST_USER_ID);
        stored.setScope(TEST_SCOPE);
        stored.setSingleUse(true);
        stored.setUsed(false);
        stored.setExpiresAt(LocalDateTime.now().minusSeconds(60)); // 1 分钟前过期
        stored.setRevoked(false);
        when(mapper.selectByHash(tokenHash)).thenReturn(stored);

        // when + then
        BizException ex = assertThrows(BizException.class, () ->
                service.verifyAndConsume(token, TEST_SCOPE, TEST_IP, TEST_UA, false));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    @Order(7)
    @DisplayName("TC-07: scope 不匹配 — 抛 'scope 不匹配' 异常")
    void tc07_verify_scopeMismatch() {
        // given: token 签发 scope=tenant:delete, 但校验时要求 user:delete
        String token = "scope-mismatch-123";
        String tokenHash = DigestUtils.sha256Hex(token);
        StepUpToken stored = new StepUpToken();
        stored.setId(45L);
        stored.setTokenHash(tokenHash);
        stored.setUserId(TEST_USER_ID);
        stored.setScope("tenant:delete");
        stored.setSingleUse(true);
        stored.setUsed(false);
        stored.setExpiresAt(LocalDateTime.now().plusSeconds(300));
        stored.setRevoked(false);
        when(mapper.selectByHash(tokenHash)).thenReturn(stored);

        // when + then
        BizException ex = assertThrows(BizException.class, () ->
                service.verifyAndConsume(token, "user:delete", TEST_IP, TEST_UA, false));
        assertTrue(ex.getMessage().contains("scope"));
    }

    @Test
    @Order(8)
    @DisplayName("TC-08: 改密码后 revokeAllUnused — 该用户所有未用 token 全部 revoked")
    void tc08_revoke_passwordChange() {
        // given: mapper 返回受影响行数 2
        when(mapper.revokeAllUnused(eq(TEST_USER_ID), any(LocalDateTime.class), eq("改密码自动撤销")))
                .thenReturn(2);

        // when
        int rows = service.revokeAllUnused(TEST_USER_ID, "改密码自动撤销");

        // then
        assertEquals(2, rows);
        verify(mapper, times(1)).revokeAllUnused(
                eq(TEST_USER_ID), any(LocalDateTime.class), eq("改密码自动撤销"));
    }
}
