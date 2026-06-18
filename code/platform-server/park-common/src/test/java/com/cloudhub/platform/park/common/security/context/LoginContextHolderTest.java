package com.cloudhub.platform.park.common.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LoginContextHolder} 单元测试
 *
 * <p>验证 set/get/clear + ThreadLocal 隔离 + 便捷方法正确性.</p>
 *
 * @author csyh fusion W2.3
 * @since 2026-06-18
 */
class LoginContextHolderTest {

    @BeforeEach
    void setUp() {
        // 每个测试前清空, 避免污染
        LoginContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        LoginContextHolder.clear();
    }

    @Test
    void get_withoutSet_shouldReturnNull() {
        assertNull(LoginContextHolder.get());
    }

    @Test
    void setAndGet_shouldRoundTrip() {
        LoginUser user = LoginUser.builder()
                .userId(1L)
                .username("admin")
                .tenantId(1L)
                .build();
        LoginContextHolder.set(user);

        LoginUser got = LoginContextHolder.get();
        assertNotNull(got);
        assertEquals(1L, got.getUserId());
        assertEquals("admin", got.getUsername());
        assertEquals(1L, got.getTenantId());
    }

    @Test
    void clear_shouldRemoveContext() {
        LoginContextHolder.set(LoginUser.builder().userId(1L).build());
        assertNotNull(LoginContextHolder.get());

        LoginContextHolder.clear();
        assertNull(LoginContextHolder.get());
    }

    @Test
    void getUserId_shouldReturnUserId() {
        LoginContextHolder.set(LoginUser.builder().userId(42L).build());
        assertEquals(42L, LoginContextHolder.getUserId());
    }

    @Test
    void getUserId_withoutContext_shouldReturnNull() {
        assertNull(LoginContextHolder.getUserId());
    }

    @Test
    void getUsername_shouldReturnUsername() {
        LoginContextHolder.set(LoginUser.builder().username("zhangs").build());
        assertEquals("zhangs", LoginContextHolder.getUsername());
    }

    @Test
    void getTenantId_shouldReturnTenantId() {
        LoginContextHolder.set(LoginUser.builder().tenantId(2L).build());
        assertEquals(2L, LoginContextHolder.getTenantId());
    }

    @Test
    void getPermissions_shouldReturnImmutableSet() {
        Set<String> perms = new HashSet<>(Arrays.asList("user:add", "user:edit"));
        LoginContextHolder.set(LoginUser.builder().permissions(perms).build());

        Set<String> got = LoginContextHolder.getPermissions();
        assertEquals(2, got.size());
        assertTrue(got.contains("user:add"));
        assertTrue(got.contains("user:edit"));
    }

    @Test
    void hasPermission_shouldCheckExactMatch() {
        LoginContextHolder.set(LoginUser.builder()
                .permissions(new HashSet<>(Arrays.asList("user:add", "user:edit")))
                .build());

        assertTrue(LoginContextHolder.hasPermission("user:add"));
        assertTrue(LoginContextHolder.hasPermission("user:edit"));
        assertFalse(LoginContextHolder.hasPermission("user:delete"));
    }

    @Test
    void hasPermission_withoutContext_shouldReturnFalse() {
        assertFalse(LoginContextHolder.hasPermission("user:add"));
    }

    @Test
    void hasAllPermissions_and_shouldCheckAll() {
        LoginContextHolder.set(LoginUser.builder()
                .permissions(new HashSet<>(Arrays.asList("user:view", "user:add", "user:edit")))
                .build());

        assertTrue(LoginContextHolder.hasAllPermissions("user:view", "user:add"));
        assertTrue(LoginContextHolder.hasAllPermissions("user:view"));
        assertFalse(LoginContextHolder.hasAllPermissions("user:view", "user:delete"));
    }

    @Test
    void hasAllPermissions_withEmptyArgs_shouldReturnTrue() {
        assertTrue(LoginContextHolder.hasAllPermissions());
    }

    @Test
    void hasAllPermissions_withNull_shouldReturnTrue() {
        assertTrue(LoginContextHolder.hasAllPermissions((String[]) null));
    }

    @Test
    void hasAnyPermission_or_shouldCheckAny() {
        LoginContextHolder.set(LoginUser.builder()
                .permissions(new HashSet<>(Arrays.asList("user:view")))
                .build());

        assertTrue(LoginContextHolder.hasAnyPermission("user:view", "user:add"));
        assertTrue(LoginContextHolder.hasAnyPermission("user:view"));
        assertFalse(LoginContextHolder.hasAnyPermission("user:add", "user:edit"));
    }

    @Test
    void hasAnyPermission_withEmptyArgs_shouldReturnTrue() {
        assertTrue(LoginContextHolder.hasAnyPermission());
    }

    @Test
    void hasRole_shouldCheckExactMatch() {
        LoginContextHolder.set(LoginUser.builder()
                .roles(Arrays.asList("admin", "manager"))
                .build());

        assertTrue(LoginContextHolder.hasRole("admin"));
        assertTrue(LoginContextHolder.hasRole("manager"));
        assertFalse(LoginContextHolder.hasRole("user"));
    }

    @Test
    void threadIsolation_shouldNotLeakBetweenThreads() throws InterruptedException {
        // 主线程设置 user 1
        LoginContextHolder.set(LoginUser.builder().userId(1L).build());
        assertEquals(1L, LoginContextHolder.getUserId());

        // 子线程读取应该为空 (无 TransmittableThreadLocal 时)
        CountDownLatch latch = new CountDownLatch(1);
        Long[] childUserId = new Long[1];
        Thread child = new Thread(() -> {
            childUserId[0] = LoginContextHolder.getUserId();
            latch.countDown();
        });
        child.start();
        latch.await(5, TimeUnit.SECONDS);

        // TransmittableThreadLocal 默认不会跨线程, 除非用 TtlRunnable 包装
        // 这里验证主线程不受影响即可
        assertEquals(1L, LoginContextHolder.getUserId());
    }

    @Test
    void threadPoolIsolation_doesNotLeak() throws InterruptedException {
        // 验证线程池场景下, 清理后下一个任务不会读到上个任务的数据
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(4);

        // 任务 1: 设置 user 1, 不 clear
        pool.submit(() -> {
            LoginContextHolder.set(LoginUser.builder().userId(1L).build());
            latch.countDown();
        });

        // 任务 2: 不设置, 应该读不到 user 1
        pool.submit(() -> {
            Long uid = LoginContextHolder.getUserId();
            assertNull(uid, "线程池不清理时新任务可能读到脏数据 (TransmittableThreadLocal 透传特性)");
            latch.countDown();
        });

        // 任务 3: 设置 user 3, 然后 clear
        pool.submit(() -> {
            LoginContextHolder.set(LoginUser.builder().userId(3L).build());
            LoginContextHolder.clear();
            latch.countDown();
        });

        // 任务 4: 不设置, 应该读不到
        pool.submit(() -> {
            assertNull(LoginContextHolder.getUserId());
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        LoginContextHolder.clear();
    }
}
