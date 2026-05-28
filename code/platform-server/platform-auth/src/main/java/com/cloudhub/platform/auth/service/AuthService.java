package com.cloudhub.platform.auth.service;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.auth.domain.vo.AuthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Random;
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

    public AuthVO loginByPassword(String username, String password) {
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
            return generateAuthVO(userId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用用户服务验证失败: {}", e.getMessage());
            throw new BizException("认证服务不可用，请稍后重试");
        }
    }

    public void sendSmsCode(String mobile) {
        String code = String.format("%06d", new Random().nextInt(999999));
        String key = SMS_CODE_PREFIX + mobile;
        redisTemplate.opsForValue().set(key, code, SMS_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("[SMS Code] {} -> {}", mobile, code);
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
            return generateAuthVO(userId);
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
        return generateAuthVO(userId);
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

    private AuthVO generateAuthVO(String userId) {
        String token = JwtUtil.generate(userId, TOKEN_EXPIRE_SECONDS);
        long expireTime = System.currentTimeMillis() + TOKEN_EXPIRE_SECONDS * 1000;
        AuthVO vo = new AuthVO();
        vo.setToken(token);
        vo.setExpireTime(expireTime);
        vo.setUserId(Long.parseLong(userId));
        return vo;
    }
}