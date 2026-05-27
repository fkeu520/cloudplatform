package com.cloudhub.platform.auth.service;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.auth.domain.vo.AuthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_PREFIX = "auth:sms:";
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";
    private static final long SMS_CODE_EXPIRE_SECONDS = 300; // 5分钟
    private static final long TOKEN_EXPIRE_SECONDS = 7 * 24 * 3600L; // 7天

    /**
     * 账号密码登录
     */
    public AuthVO loginByPassword(String username, String password) {
        // TODO: 调用 user-center RPC 或者 restTemplate 查询用户
        // 临时模拟：只有 admin/123456 能登录
        if (!"admin".equals(username) || !"123456".equals(password)) {
            throw new BizException("用户名或密码错误");
        }

        String userId = "1"; // 模拟用户ID
        return generateAuthVO(userId);
    }

    /**
     * 发送短信验证码
     */
    public void sendSmsCode(String mobile) {
        // 模拟发送：生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        String key = SMS_CODE_PREFIX + mobile;
        redisTemplate.opsForValue().set(key, code, SMS_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("【短信验证码】{} -> {}", mobile, code); // 实际生产不打印
    }

    /**
     * 短信验证码登录
     */
    public AuthVO loginBySms(String mobile, String code) {
        String key = SMS_CODE_PREFIX + mobile;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!cached.equals(code)) {
            throw new BizException("验证码错误");
        }
        // 验证通过，删除验证码
        redisTemplate.delete(key);

        // TODO: 根据手机号查询用户，不存在则自动注册
        String userId = "1"; // 模拟
        return generateAuthVO(userId);
    }

    /**
     * 刷新Token
     */
    public AuthVO refreshToken(String token) {
        // 简单处理：解析旧token，返回新token
        if (!JwtUtil.validate(token)) {
            throw new BizException("Token无效");
        }
        String userId = JwtUtil.getUserId(token);
        return generateAuthVO(userId);
    }

    /**
     * 验证Token
     */
    public void validateToken(String token) {
        if (!JwtUtil.validate(token)) {
            throw new BizException("Token无效或已过期");
        }
    }

    /**
     * 退出登录
     */
    public void logout(String token) {
        try {
            String userId = JwtUtil.getUserId(token);
            long expire = JwtUtil.parse(token).getExpiration().getTime() - System.currentTimeMillis();
            if (expire > 0) {
                // 将token加入黑名单，过期时间与剩余TTL一致
                String key = TOKEN_BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(key, userId, expire, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("退出登录解析Token失败: {}", e.getMessage());
        }
    }

    // ========== 内部方法 ==========

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