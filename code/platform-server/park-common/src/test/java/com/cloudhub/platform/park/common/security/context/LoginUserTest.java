package com.cloudhub.platform.park.common.security.context;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LoginUser} DTO 单元测试
 */
class LoginUserTest {

    @Test
    void builder_shouldCreateLoginUser() {
        Set<String> perms = new HashSet<>(Arrays.asList("user:add", "user:edit"));
        LoginUser user = LoginUser.builder()
                .userId(1L)
                .username("admin")
                .tenantId(1L)
                .roles(Arrays.asList("admin", "manager"))
                .permissions(perms)
                .build();

        assertNotNull(user);
        assertEquals(1L, user.getUserId());
        assertEquals("admin", user.getUsername());
        assertEquals(1L, user.getTenantId());
        assertEquals(2, user.getRoles().size());
        assertEquals(2, user.getPermissions().size());
        assertTrue(user.getPermissions().contains("user:add"));
    }

    @Test
    void builder_defaultsEmptyCollections() {
        LoginUser user = LoginUser.builder()
                .userId(1L)
                .username("test")
                .build();

        assertNotNull(user.getRoles());
        assertNotNull(user.getPermissions());
        assertEquals(0, user.getRoles().size());
        assertEquals(0, user.getPermissions().size());
    }

    @Test
    void noArgsConstructor_shouldWork() {
        LoginUser user = new LoginUser();
        user.setUserId(2L);
        user.setUsername("zhangs");
        assertEquals(2L, user.getUserId());
        assertEquals("zhangs", user.getUsername());
    }
}
