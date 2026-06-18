package com.cloudhub.platform.park.common.security.aspect;

import com.cloudhub.platform.park.common.base.exception.AccessDeniedException;
import com.cloudhub.platform.park.common.security.annotation.Logical;
import com.cloudhub.platform.park.common.security.annotation.RequiresPermissions;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.park.common.security.context.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RequiresPermissionsAspect} AOP 单元测试
 *
 * <p>使用 Spring AOP {@link AspectJProxyFactory} 手动创建代理, 验证:
 * <ul>
 *   <li>无 LoginUser → 抛 403</li>
 *   <li>有 LoginUser, 权限不足 → 抛 403</li>
 *   <li>有 LoginUser, 权限满足 → 放行</li>
 *   <li>Logical.AND / Logical.OR 行为</li>
 *   <li>类级 + 方法级注解组合</li>
 * </ul>
 *
 * <p>使用 {@code AspectJProxyFactory} 而非 {@code @SpringBootTest}, 因为:
 * <ol>
 *   <li>park-common 是 library 模块, 没有主启动类</li>
 *   <li>不依赖 Spring Boot 上下文, 单元测试更轻量</li>
 *   <li>AOP 织入逻辑通过 ProxyFactory 即可验证</li>
 * </ol>
 *
 * @author csyh fusion W2.3
 * @since 2026-06-18
 */
class RequiresPermissionsAspectTest {

    private TestService testService;
    private ClassLevelTestService classLevelService;

    @BeforeEach
    void setUp() {
        // 使用 AspectJProxyFactory 手动织入切面
        TestService target = new TestService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new RequiresPermissionsAspect());
        testService = factory.getProxy();

        ClassLevelTestService classTarget = new ClassLevelTestService();
        AspectJProxyFactory classFactory = new AspectJProxyFactory(classTarget);
        classFactory.addAspect(new RequiresPermissionsAspect());
        classLevelService = classFactory.getProxy();

        LoginContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        LoginContextHolder.clear();
    }

    // ========== 无 LoginUser 测试 ==========

    @Test
    void noLoginUser_shouldDeny() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> testService.singlePermission());
        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("未登录") || ex.getMessage().contains("权限不足"));
    }

    // ========== 权限不足测试 ==========

    @Test
    void insufficientPermission_shouldDeny() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L).username("user1")
                .permissions(new HashSet<>(Arrays.asList("user:view")))
                .build());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> testService.singlePermission());  // 需要 user:add
        assertEquals(403, ex.getCode());
    }

    // ========== 权限满足测试 ==========

    @Test
    void sufficientPermission_shouldAllow() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L).username("admin")
                .permissions(new HashSet<>(Arrays.asList("user:add", "user:view")))
                .build());

        assertDoesNotThrow(() -> testService.singlePermission());
    }

    // ========== Logical.AND 测试 ==========

    @Test
    void andAllMatch_shouldAllow() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L)
                .permissions(new HashSet<>(Arrays.asList("user:view", "user:edit")))
                .build());

        assertDoesNotThrow(() -> testService.multipleAndPermission());
    }

    @Test
    void andPartialMatch_shouldDeny() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L)
                .permissions(new HashSet<>(Arrays.asList("user:view")))  // 缺 user:edit
                .build());

        assertThrows(AccessDeniedException.class,
                () -> testService.multipleAndPermission());
    }

    // ========== Logical.OR 测试 ==========

    @Test
    void orAnyMatch_shouldAllow() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L)
                .permissions(new HashSet<>(Arrays.asList("user:view")))  // 满足任一
                .build());

        assertDoesNotThrow(() -> testService.multipleOrPermission());
    }

    @Test
    void orNoMatch_shouldDeny() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L)
                .permissions(new HashSet<>(Arrays.asList("user:delete")))  // 都不满足
                .build());

        assertThrows(AccessDeniedException.class,
                () -> testService.multipleOrPermission());
    }

    // ========== 类级注解测试 ==========

    @Test
    void classLevelAnnotation_shouldApply() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L)
                .permissions(new HashSet<>(Arrays.asList("class:level")))  // 满足类级
                .build());

        assertDoesNotThrow(() -> classLevelService.classLevelOnlyMethod());
    }

    @Test
    void classLevelAnnotation_deny_shouldDeny() {
        LoginContextHolder.set(LoginUser.builder()
                .userId(1L)
                .permissions(new HashSet<>(Arrays.asList("other:perm")))  // 不满足类级
                .build());

        assertThrows(AccessDeniedException.class,
                () -> classLevelService.classLevelOnlyMethod());
    }

    @Test
    void noAnnotation_shouldAlwaysAllow() {
        // 不设 LoginContextHolder 也应通过 (无任何注解, 切面不拦截)
        assertDoesNotThrow(() -> testService.noAnnotation());
    }

    // ========== 测试服务 ==========

    /**
     * 测试用服务, 演示各类注解用法
     */
    public static class TestService {

        @RequiresPermissions("user:add")
        public void singlePermission() {
            // 方法级单权限
        }

        @RequiresPermissions(value = {"user:view", "user:edit"}, logical = Logical.AND)
        public void multipleAndPermission() {
            // AND 关系
        }

        @RequiresPermissions(value = {"user:view", "user:export"}, logical = Logical.OR)
        public void multipleOrPermission() {
            // OR 关系
        }

        /**
         * 不带任何注解 (无类级 + 无方法级)
         */
        public void noAnnotation() {
            // 自由访问, 切面不拦截
        }
    }

    /**
     * 类级注解测试用服务
     */
    @RequiresPermissions("class:level")
    public static class ClassLevelTestService {

        public void classLevelOnlyMethod() {
            // 继承类的 @RequiresPermissions("class:level")
        }
    }
}
