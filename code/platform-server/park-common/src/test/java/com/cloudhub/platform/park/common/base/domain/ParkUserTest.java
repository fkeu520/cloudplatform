package com.cloudhub.platform.park.common.base.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParkUser 字段测试 (W2.1 验证)
 *
 * <p>验证 csyh User 字段全部翻译到位, 业务代码改 import 后字段名不变.</p>
 */
class ParkUserTest {

    @Test
    void testFieldCompatibility() {
        ParkUser user = new ParkUser();
        user.setId(100L);
        user.setUsername("zhangs");
        user.setName("张三");
        user.setTenantId(1L);
        user.setOrgId(10L);
        user.setDeptId(20L);
        user.setPostId(30L);
        user.setEmail("zhangs@example.com");
        user.setPhone("13800138000");
        user.setStatus(0);
        user.setCreateTime(LocalDateTime.now());
        user.setRoles(List.of("admin", "user"));
        user.setPermissions(Set.of("user:add", "user:edit"));

        // csyh 字段全部存在且类型正确
        assertEquals(100L, user.getId());
        assertEquals("zhangs", user.getUsername());
        assertEquals("张三", user.getName());
        assertEquals(1L, user.getTenantId());
        assertEquals(10L, user.getOrgId());
        assertEquals(20L, user.getDeptId());
        assertEquals(30L, user.getPostId());
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getPermissions().contains("user:add"));
    }

    @Test
    void testDefaultConstructor() {
        ParkUser user = new ParkUser();
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getRoles());
        assertNull(user.getPermissions());
    }
}
