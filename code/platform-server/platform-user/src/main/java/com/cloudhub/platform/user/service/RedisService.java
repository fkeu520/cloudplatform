package com.cloudhub.platform.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // ========== String 操作 ==========

    /**
     * 设置缓存
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置缓存，指定秒级过期
     */
    public void setEx(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    /**
     * 获取缓存
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     */
    public Long del(String... keys) {
        return redisTemplate.delete(java.util.Arrays.asList(keys));
    }

    // ========== 计数操作（限流用）============

    /**
     * 自增
     */
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增并设置过期
     */
    public Long incr(String key, long delta, long timeout, TimeUnit unit) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        redisTemplate.expire(key, timeout, unit);
        return result;
    }

    // ========== Key 操作 ==========

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取剩余过期时间（秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // ========== 分布式锁 ==========

    /**
     * 尝试获取锁（简单实现，生产建议用 Redisson）
     *
     * @param key        锁 key
     * @param value      锁 value（通常用 UUID）
     * @param expireTime 过期时间
     * @return true=获取成功
     */
    public boolean tryLock(String key, String value, long expireTime) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(expireTime));
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放锁（必须判断 value 匹配才能释放，防止误删他人的锁）
     */
    public void unlock(String key, String value) {
        String current = get(key);
        if (value.equals(current)) {
            del(key);
        }
    }
}