package com.cloudhub.platform.auth.service;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.common.util.RsaUtil;
import com.cloudhub.platform.auth.domain.vo.AuthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    private static final String SMS_CODE_PREFIX = "auth:sms:";
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";
    private static final long SMS_CODE_EXPIRE_SECONDS = 300;
    private static final long TOKEN_EXPIRE_SECONDS = 7 * 24 * 3600L;

    public AuthVO loginByPassword(String username, String encryptedPassword) {
        // 解密前端传来的 RSA 加密密码
        String password;
        try {
            password = RsaUtil.decrypt(encryptedPassword);
        } catch (Exception e) {
            log.warn("密码解密失败，可能未加密或加密方式错误: {}", e.getMessage());
            // 兼容未加密的情况（开发阶段）
            password = encryptedPassword;
        }

        String url = userServiceUrl + "/user/internal/validate";
        Map<String, String> body = Map.of("username", username, "password", password);
        try {
            Map<String, Object> result = restTemplate.postForObject(url, body, Map.class);
            if (result == null || !Integer.valueOf(200).equals(result.get("code"))) {
                String msg = result != null ? (String) result.getOrDefault("message", "用户名或密码错误") : "认证服务不可用";
                throw new BizException(msg);
            }
            Map<String, Object> userData = (Map<String, Object>) result.get("data");
            String userId = String.valueOf(userData.get("id"));
            Number tenantNum = (Number) userData.get("tenantId");
            Long tenantId = tenantNum != null ? tenantNum.longValue() : 0L;
            return generateAuthVO(userId, tenantId, userData);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用用户服务验证失败: {}", e.getMessage());
            throw new BizException("认证服务不可用，请稍后重试");
        }
    }

    public void sendSmsCode(String mobile) {
        // 频率限制：同一手机号每分钟最多1次
        String rateLimitKey = SMS_CODE_PREFIX + "rate:" + mobile;
        Boolean exists = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BizException("验证码发送过于频繁，请稍后再试");
        }
        // 使用 SecureRandom 替代 Random
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
        String key = SMS_CODE_PREFIX + mobile;
        redisTemplate.opsForValue().set(key, code, SMS_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        // 设置频率限制（60秒）
        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
        log.info("[SMS Code] {} -> ****** (code sent)", mobile);
    }

    public AuthVO loginBySms(String mobile, String code) {
        String key = SMS_CODE_PREFIX + mobile;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new BizException("验证码已过期，请重新发送");
        }
        if (!cached.equals(code)) {
            throw new BizException("验证码错误");
        }
        redisTemplate.delete(key);
        String url = userServiceUrl + "/user/internal/by-username/" + mobile;
        try {
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            if (result == null || !Integer.valueOf(200).equals(result.get("code"))) {
                throw new BizException("手机号未注册，请先注册");
            }
            Map<String, Object> userData = (Map<String, Object>) result.get("data");
            String userId = String.valueOf(userData.get("id"));
            Number tenantNum = (Number) userData.get("tenantId");
            Long tenantId = tenantNum != null ? tenantNum.longValue() : 0L;
            return generateAuthVO(userId, tenantId, userData);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用用户服务查询失败: {}", e.getMessage());
            throw new BizException("认证服务不可用，请稍后重试");
        }
    }

    public AuthVO refreshToken(String token) {
        if (!JwtUtil.validate(token)) {
            throw new BizException("Invalid token");
        }
        String userId = JwtUtil.getUserId(token);
        Long tenantId = JwtUtil.getTenantId(token);
        return generateAuthVO(userId, tenantId != null ? tenantId : 0L);
    }

    public void validateToken(String token) {
        if (!JwtUtil.validate(token)) {
            throw new BizException("Invalid or expired token");
        }
    }

    public void logout(String token) {
        try {
            String userId = JwtUtil.getUserId(token);
            long expire = JwtUtil.parse(token).getExpiration().getTime() - System.currentTimeMillis();
            if (expire > 0) {
                String key = TOKEN_BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(key, userId, expire, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("Logout parse token failed: {}", e.getMessage());
        }
    }

    private AuthVO generateAuthVO(String userId, Long tenantId) {
        return generateAuthVO(userId, tenantId, null);
    }

    private AuthVO generateAuthVO(String userId, Long tenantId, Map<String, Object> userData) {
        // 从 userData 中获取 userType
        Integer userType = null;
        if (userData != null && userData.get("userType") instanceof Number) {
            userType = ((Number) userData.get("userType")).intValue();
        }
        // 从 userData 中获取 username, 用于 token 中携带
        String username = (userData != null && userData.get("username") instanceof String)
            ? (String) userData.get("username") : null;
        String token = JwtUtil.generate(userId, username, TOKEN_EXPIRE_SECONDS);
        if (tenantId != null && tenantId > 0) {
            token = JwtUtil.generate(userId, username, tenantId, userType, TOKEN_EXPIRE_SECONDS);
        }
        long expireTime = System.currentTimeMillis() + TOKEN_EXPIRE_SECONDS * 1000;
        AuthVO vo = new AuthVO();
        vo.setToken(token);
        vo.setExpireTime(expireTime);
        vo.setUserId(Long.parseLong(userId));
        vo.setUser(userData);
        return vo;
    }
}