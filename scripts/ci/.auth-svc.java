rtk : [rtk] /!\ No hook installed 鈥?run `rtk init -g` for automatic token savings
所在位置 行:1 字符: 400
+ ... LS='false'; rtk git show feat/m5-p0-2-pr4-write-strict:code/platform- ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: ([rtk] /!\ No ho...c token savings:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
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
        // 瑙ｅ瘑鍓嶇浼犳潵鐨?RSA 鍔犲瘑瀵嗙爜
        String password;
        try {
            password = RsaUtil.decrypt(encryptedPassword);
        } catch (Exception e) {
            log.warn("瀵嗙爜瑙ｅ瘑澶辫触锛屽彲鑳芥湭鍔犲瘑鎴栧姞瀵嗘柟寮忛敊璇? {}", e.getMessage());
            // 鍏煎鏈姞瀵嗙殑鎯呭喌锛堝紑鍙戦樁娈碉級
            password = encryptedPassword;
        }

        String url = userServiceUrl + "/user/internal/validate";
        Map<String, String> body = Map.of("username", username, "password", password);
        try {
            Map<String, Object> result = restTemplate.postForObject(url, body, Map.class);
            if (result == null || !Integer.valueOf(200).equals(result.get("code"))) {
                String msg = result != null ? (String) result.getOrDefault("message", "鐢ㄦ埛鍚嶆垨瀵嗙爜閿欒") : "璁よ瘉鏈嶅姟涓嶅彲鐢?;
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
            log.warn("璋冪敤鐢ㄦ埛鏈嶅姟楠岃瘉澶辫触: {}", e.getMessage());
            throw new BizException("璁よ瘉鏈嶅姟涓嶅彲鐢紝璇风◢鍚庨噸璇?);
        }
    }

    public void sendSmsCode(String mobile) {
        // 棰戠巼闄愬埗锛氬悓涓€鎵嬫満鍙锋瘡鍒嗛挓鏈€澶?娆?        String rateLimitKey = SMS_CODE_PREFIX + "rate:" + mobile;
        Boolean exists = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BizException("楠岃瘉鐮佸彂閫佽繃浜庨绻侊紝璇风◢鍚庡啀璇?);
        }
        // 浣跨敤 SecureRandom 鏇夸唬 Random
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
        String key = SMS_CODE_PREFIX + mobile;
        redisTemplate.opsForValue().set(key, code, SMS_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        // 璁剧疆棰戠巼闄愬埗锛?0绉掞級
        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
        log.info("[SMS Code] {} -> ****** (code sent)", mobile);
    }

    public AuthVO loginBySms(String mobile, String code) {
        String key = SMS_CODE_PREFIX + mobile;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new BizException("楠岃瘉鐮佸凡杩囨湡锛岃閲嶆柊鍙戦€?);
        }
        if (!cached.equals(code)) {
            throw new BizException("楠岃瘉鐮侀敊璇?);
        }
        redisTemplate.delete(key);
        String url = userServiceUrl + "/user/internal/by-username/" + mobile;
        try {
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            if (result == null || !Integer.valueOf(200).equals(result.get("code"))) {
                throw new BizException("鎵嬫満鍙锋湭娉ㄥ唽锛岃鍏堟敞鍐?);
            }
            Map<String, Object> userData = (Map<String, Object>) result.get("data");
            String userId = String.valueOf(userData.get("id"));
            Number tenantNum = (Number) userData.get("tenantId");
            Long tenantId = tenantNum != null ? tenantNum.longValue() : 0L;
            return generateAuthVO(userId, tenantId, userData);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("璋冪敤鐢ㄦ埛鏈嶅姟鏌ヨ澶辫触: {}", e.getMessage());
            throw new BizException("璁よ瘉鏈嶅姟涓嶅彲鐢紝璇风◢鍚庨噸璇?);
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
        // 浠?userData 涓幏鍙?userType
        Integer userType = null;
        if (userData != null && userData.get("userType") instanceof Number) {
            userType = ((Number) userData.get("userType")).intValue();
        }
        String token = JwtUtil.generate(userId, TOKEN_EXPIRE_SECONDS);
        if (tenantId != null && tenantId > 0) {
            token = JwtUtil.generate(userId, "", tenantId, userType, TOKEN_EXPIRE_SECONDS);
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
