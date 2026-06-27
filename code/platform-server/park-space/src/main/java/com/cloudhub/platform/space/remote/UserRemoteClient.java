package com.cloudhub.platform.space.remote;

import com.cloudhub.platform.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 跨服务调用 platform-user (取用户昵称)
 * <p>通过 Nacos 服务名 + @LoadBalanced RestTemplate 调用</p>
 */
@Slf4j
@Component
public class UserRemoteClient {

    private static final String USER_SERVICE = "platform-user";
    private static final String BY_USERNAME_URL =
            "http://" + USER_SERVICE + "/user/internal/by-username/{username}";

    private final RestTemplate restTemplate;

    public UserRemoteClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 根据登录账号取昵称
     * @param username 登录账号
     * @return nickname (失败/不存在返回 null)
     */
    public String getNickname(String username) {
        if (username == null || username.isBlank()) return null;
        try {
            Result<UserInfoVO> resp = restTemplate.getForObject(
                    BY_USERNAME_URL, Result.class, username);
            if (resp == null || resp.getData() == null) return null;
            // Jackson 反序列化 Result<UserInfoVO> 时 data 可能是 LinkedHashMap, 手动提取
            Object data = resp.getData();
            if (data instanceof UserInfoVO) {
                return ((UserInfoVO) data).getNickname();
            }
            // 兜底: 反射拿 nickname
            try {
                Object nick = data.getClass().getMethod("getNickname").invoke(data);
                return nick == null ? null : nick.toString();
            } catch (Exception reflectEx) {
                log.warn("[UserRemoteClient] nickname 反射失败: {}", reflectEx.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.warn("[UserRemoteClient] 调用 platform-user by-username 失败 username={}: {}",
                    username, e.getMessage());
            return null;
        }
    }
}