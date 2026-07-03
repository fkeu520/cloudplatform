package com.cloudhub.platform.auth.service;

import com.cloudhub.platform.auth.domain.StepUpToken;
import com.cloudhub.platform.auth.domain.mapper.StepUpTokenMapper;
import com.cloudhub.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Step-up Token 服务
 * <p>5 分钟有效 + 单次使用 + scope 限定 + IP/UA 绑定.</p>
 * <p>配套: doc/plan/v8-P0-tenant-protection-plan.md ADR-008</p>
 * <p>Day 1 单元测试 8 TC, 集成测试 5 TC 留作 Day 5.</p>
 *
 * @author cloudhub
 * @since v8.0 (2026-07)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StepUpTokenService {

    private final StepUpTokenMapper mapper;
    private final StringRedisTemplate redis;
    private final RestTemplate restTemplate;

    @Value("${stepup.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${stepup.max-issues-per-window:3}")
    private int maxIssuesPerWindow;

    @Value("${stepup.rate-limit-seconds:300}")
    private long rateLimitSeconds;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    private static final String RATE_LIMIT_KEY = "auth:stepup:rate:";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 签发 step-up token
     * <p><b>注意</b>: {@code plainPassword} 必须是 RSA 解密后的明文密码.
     * Controller 层应在调用前用 {@code RsaUtil.decrypt} 解密前端传来的密文.</p>
     *
     * @param userId        用户ID
     * @param tenantId      租户ID (平台超管为 null)
     * @param scope         操作范围 (如 "tenant:delete")
     * @param singleUse     是否单次有效
     * @param plainPassword 明文密码 (强鉴权凭证, 已由 Controller 层 RSA 解密)
     * @param ip            客户端 IP
     * @param ua            客户端 UA
     */
    @Transactional
    public IssueResult issue(Long userId, Long tenantId, String scope,
                             boolean singleUse, String plainPassword,
                             String ip, String ua) {
        if (userId == null) {
            throw new BizException("userId 不能为空");
        }
        if (scope == null || scope.isBlank()) {
            throw new BizException("scope 不能为空");
        }

        // 1. 频率限制: 5 分钟内最多 3 次
        String rateKey = RATE_LIMIT_KEY + userId;
        String current = redis.opsForValue().get(rateKey);
        int issued = current == null ? 0 : Integer.parseInt(current);
        if (issued >= maxIssuesPerWindow) {
            throw new BizException("Step-up token 申请过于频繁, 请稍后再试");
        }
        redis.opsForValue().set(rateKey, String.valueOf(issued + 1),
                rateLimitSeconds, TimeUnit.SECONDS);

        // 2. 强鉴权: 验证密码
        validatePassword(userId, plainPassword);

        // 3. 生成 token (32 字节随机, Base64 URL 安全编码)
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = DigestUtils.sha256Hex(token);

        // 4. 持久化 (存哈希不存明文)
        StepUpToken entity = new StepUpToken();
        entity.setTokenHash(tokenHash);
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setScope(scope);
        entity.setSingleUse(singleUse);
        entity.setUsed(false);
        entity.setIssuedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        entity.setClientIp(ip);
        entity.setUserAgent(ua == null ? null : (ua.length() > 500 ? ua.substring(0, 500) : ua));
        entity.setRevoked(false);
        mapper.insert(entity);

        log.info("Step-up token issued: userId={}, scope={}, singleUse={}, expiresAt={}",
                userId, scope, singleUse, entity.getExpiresAt());

        return new IssueResult(token, entity.getExpiresAt(), singleUse, scope);
    }

    /**
     * 校验并消费 token
     * <p>校验项: 哈希匹配 / 未过期 / 未撤销 / scope 匹配 / IP-UA 绑定 / 单次使用标记</p>
     * @return tokenId (供 OperLogAspect 写入审计)
     */
    @Transactional
    public Long verifyAndConsume(String token, String requiredScope,
                                 String requestIp, String requestUa,
                                 boolean allowMultiUse) {
        if (token == null || token.isBlank()) {
            throw new BizException("缺少 X-Step-Up-Token 头");
        }
        String tokenHash = DigestUtils.sha256Hex(token);
        StepUpToken entity = mapper.selectByHash(tokenHash);
        if (entity == null) {
            throw new BizException("Step-up token 无效");
        }
        // 1. 过期
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException("Step-up token 已过期, 请重新认证");
        }
        // 2. 撤销
        if (Boolean.TRUE.equals(entity.getRevoked())) {
            throw new BizException("Step-up token 已被撤销");
        }
        // 3. scope
        if (!scopeMatches(entity.getScope(), requiredScope)) {
            throw new BizException("Step-up token scope 不匹配: 需要 " + requiredScope
                    + ", 实际 " + entity.getScope());
        }
        // 4. IP/UA 绑定 (防劫持)
        if (entity.getClientIp() != null && !entity.getClientIp().equals(requestIp)) {
            log.warn("Step-up token IP mismatch: expected={}, actual={}",
                    entity.getClientIp(), requestIp);
            throw new BizException("Step-up token 客户端不匹配");
        }
        if (entity.getUserAgent() != null && !entity.getUserAgent().equals(requestUa)) {
            throw new BizException("Step-up token 客户端不匹配");
        }
        // 5. 单次使用
        if (Boolean.TRUE.equals(entity.getSingleUse()) && !allowMultiUse) {
            if (Boolean.TRUE.equals(entity.getUsed())) {
                throw new BizException("Step-up token 已使用, 请重新认证");
            }
            // 标记已使用
            entity.setUsed(true);
            entity.setConsumedAt(LocalDateTime.now());
            mapper.updateById(entity);
        }

        log.info("Step-up token consumed: userId={}, scope={}, tokenId={}",
                entity.getUserId(), requiredScope, entity.getId());
        return entity.getId();
    }

    /**
     * 撤销某用户所有未使用的 step-up token (改密码 / 主动踢下时调用)
     * @return 受影响行数
     */
    @Transactional
    public int revokeAllUnused(Long userId, String reason) {
        if (userId == null) {
            throw new BizException("userId 不能为空");
        }
        int rows = mapper.revokeAllUnused(userId, LocalDateTime.now(), reason);
        log.info("Step-up tokens revoked: userId={}, count={}, reason={}", userId, rows, reason);
        return rows;
    }

    /**
     * 通过 user service 强鉴权密码 (明文).
     * <p>复用 AuthService.loginByPassword 的 RPC 调用模式 (RestTemplate + /user/internal/validate).</p>
     * <p>注意: 明文密码的安全边界由 {@code application.yml} 的 HTTPS 传输 + Controller 层 RSA 解密保证.</p>
     */
    private void validatePassword(Long userId, String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new BizException("密码不能为空");
        }
        try {
            // 直接调 user service 的密码验证端点 (传明文, HTTPS 保障传输安全)
            String url = userServiceUrl + "/user/internal/validate";
            var body = new java.util.HashMap<String, String>();
            body.put("userId", String.valueOf(userId));
            body.put("password", plainPassword);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> resp = restTemplate.postForObject(url, body, java.util.Map.class);
            if (resp == null || !Integer.valueOf(200).equals(resp.get("code"))) {
                throw new BizException("密码验证失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Step-up 调 user service 失败: userId={}, err={}", userId, e.getMessage());
            throw new BizException("密码验证服务不可用, 请稍后重试");
        }
    }

    private boolean scopeMatches(String tokenScope, String requiredScope) {
        if (tokenScope == null || requiredScope == null) return false;
        for (String s : tokenScope.split(",")) {
            if (s.trim().equals(requiredScope)) return true;
        }
        return false;
    }

    /** 签发结果 */
    public record IssueResult(String token, LocalDateTime expiresAt,
                              boolean singleUse, String scope) {}
}
