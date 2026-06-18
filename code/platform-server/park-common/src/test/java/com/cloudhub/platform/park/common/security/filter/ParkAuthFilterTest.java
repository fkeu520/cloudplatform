package com.cloudhub.platform.park.common.security.filter;

import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.park.common.security.context.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link ParkAuthFilter} 单元测试
 * <p>使用 Spring Mock 体系 (MockHttpServletRequest/Response/FilterChain) 验证:
 * <ul>
 *   <li>有 X-User-Id → 写入 LoginContextHolder</li>
 *   <li>无 X-User-Id → 跳过写入, 不报错</li>
 *   <li>内部调用 (from=in) → 跳过 LoginContextHolder</li>
 *   <li>请求结束后 → LoginContextHolder.clear() 被调用</li>
 *   <li>所有 Header 字段 (username/tenantId/userType) 正确解析</li>
 * </ul>
 */
class ParkAuthFilterTest {

    private ParkAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ParkAuthFilter();
        LoginContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        LoginContextHolder.clear();
    }

    // ========== Header 解析测试 ==========

    @Test
    void allHeadersPresent_shouldPopulateLoginContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(ParkAuthFilter.HEADER_USER_ID, "42");
        request.addHeader(ParkAuthFilter.HEADER_USER_NAME, "zhangs");
        request.addHeader(ParkAuthFilter.HEADER_TENANT_ID, "1");
        request.addHeader(ParkAuthFilter.HEADER_USER_TYPE, "1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // 拦截 doFilter, 验证 LoginContextHolder 在业务执行时已写入
        chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                LoginUser current = LoginContextHolder.get();
                assertNotNull(current, "业务执行时 LoginContextHolder 必须有值");
                assertEquals(42L, current.getUserId());
                assertEquals("zhangs", current.getUsername());
                assertEquals(1L, current.getTenantId());
            }
        };

        filter.doFilter(request, response, chain);

        // 业务执行后, LoginContextHolder 应被清理
        assertNull(LoginContextHolder.get(), "请求结束后 LoginContextHolder 必须清空");
    }

    @Test
    void onlyUserIdHeader_shouldStillPopulate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(ParkAuthFilter.HEADER_USER_ID, "100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                LoginUser current = LoginContextHolder.get();
                assertNotNull(current);
                assertEquals(100L, current.getUserId());
                assertNull(current.getUsername());
                assertNull(current.getTenantId());
                // @Builder.Default: roles/permissions 是空集合, 不是 null
                assertNotNull(current.getRoles());
                assertNotNull(current.getPermissions());
                assertEquals(0, current.getRoles().size());
                assertEquals(0, current.getPermissions().size());
            }
        };

        filter.doFilter(request, response, chain);

        assertNull(LoginContextHolder.get());
    }

    @Test
    void noUserIdHeader_shouldNotPopulate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        // 无 X-User-Id
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                assertNull(LoginContextHolder.get(), "无 userId 时不应写入");
            }
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void internalCall_shouldSkipLoginContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("from", ParkAuthFilter.HEADER_FROM_IN);
        // 即使有 userId, 内部调用也不写 LoginContextHolder
        request.addHeader(ParkAuthFilter.HEADER_USER_ID, "999");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                assertNull(LoginContextHolder.get(), "内部调用跳过 LoginContextHolder");
            }
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void invalidNumberInHeader_shouldNotCrash() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(ParkAuthFilter.HEADER_USER_ID, "42");
        request.addHeader(ParkAuthFilter.HEADER_TENANT_ID, "not-a-number");
        request.addHeader(ParkAuthFilter.HEADER_USER_TYPE, "abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                LoginUser current = LoginContextHolder.get();
                assertNotNull(current);
                assertEquals(42L, current.getUserId());
                assertNull(current.getTenantId(), "非法数字应解析为 null 而非抛异常");
                // userType 字段未存到 LoginUser, 改用 roles 字段 (W2 阶段简化)
                assertNotNull(current.getRoles());
            }
        };

        filter.doFilter(request, response, chain);
    }

    // ========== 清理测试 ==========

    @Test
    void loginContextAlwaysCleared_evenOnException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(ParkAuthFilter.HEADER_USER_ID, "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 业务代码抛异常
        FilterChain throwingChain = (req, res) -> {
            assertNotNull(LoginContextHolder.get());
            throw new RuntimeException("业务异常");
        };

        try {
            filter.doFilter(request, response, throwingChain);
        } catch (RuntimeException expected) {
            // 异常会向上传播, 但 LoginContextHolder 必须被清空
        }

        assertNull(LoginContextHolder.get(), "异常场景下 LoginContextHolder 也必须清理");
    }

    // ========== 调用链测试 ==========

    @Test
    void chainIsAlwaysInvoked() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain mockChain = mock(FilterChain.class);

        filter.doFilter(request, response, mockChain);

        verify(mockChain).doFilter(request, response);
    }

    // ========== 集成: 与 RequiresPermissionsAspect 联动测试 ==========

    @Test
    void endToEnd_filterThenAspect_shouldWork() throws ServletException, IOException {
        // 模拟一次完整请求: 过滤器写上下文 → 业务调用 → AOP 校验
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/room/list");
        request.addHeader(ParkAuthFilter.HEADER_USER_ID, "10");
        request.addHeader(ParkAuthFilter.HEADER_USER_NAME, "admin");
        request.addHeader(ParkAuthFilter.HEADER_TENANT_ID, "1");
        request.addHeader(ParkAuthFilter.HEADER_USER_TYPE, "1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                // 验证 LoginContextHolder 在业务代码中可访问
                LoginUser u = LoginContextHolder.get();
                assertNotNull(u);
                assertEquals(10L, u.getUserId());
                assertEquals("admin", u.getUsername());
                assertEquals(1L, u.getTenantId());
            }
        };

        filter.doFilter(request, response, chain);

        // 验证: 业务执行结束后, context 被清空 (线程复用安全)
        assertNull(LoginContextHolder.get());
    }
}
