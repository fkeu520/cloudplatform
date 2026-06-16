package com.cloudhub.platform.park.common.security.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RequiresPermissions} 注解测试 (W2.3 验证)
 */
class RequiresPermissionsTest {

    @RequiresPermissions("user:add")
    public void testSinglePermission() {}

    @RequiresPermissions(value = {"user:view", "user:edit"}, logical = Logical.AND)
    public void testMultiplePermissionsAnd() {}

    @RequiresPermissions(value = {"user:view", "user:export"}, logical = Logical.OR)
    public void testMultiplePermissionsOr() {}

    @Test
    void testAnnotationPresence() throws NoSuchMethodException {
        Method m1 = RequiresPermissionsTest.class.getMethod("testSinglePermission");
        RequiresPermissions a1 = m1.getAnnotation(RequiresPermissions.class);
        assertNotNull(a1);
        assertArrayEquals(new String[]{"user:add"}, a1.value());
        assertEquals(Logical.AND, a1.logical());
    }

    @Test
    void testMultipleAnd() throws NoSuchMethodException {
        Method m = RequiresPermissionsTest.class.getMethod("testMultiplePermissionsAnd");
        RequiresPermissions a = m.getAnnotation(RequiresPermissions.class);
        assertEquals(2, a.value().length);
        assertEquals(Logical.AND, a.logical());
    }

    @Test
    void testMultipleOr() throws NoSuchMethodException {
        Method m = RequiresPermissionsTest.class.getMethod("testMultiplePermissionsOr");
        RequiresPermissions a = m.getAnnotation(RequiresPermissions.class);
        assertEquals(2, a.value().length);
        assertEquals(Logical.OR, a.logical());
    }
}
