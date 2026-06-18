package com.cloudhub.platform.park.common.base.response;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R / AjaxResult / TableDataInfo 适配层单元测试
 * <p>验证 park-common 适配类正确委托 platform-common 统一响应, 字段/方法完全兼容.</p>
 */
class RTest {

    // ========== R 静态工厂测试 ==========

    @Test
    void r_ok_shouldReturnResultWith200Code() {
        Result<String> r = R.ok("hello");
        assertNotNull(r);
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
        assertTrue(r instanceof Result, "R.ok() 返回 Result 实例");
    }

    @Test
    void r_okWithoutData_shouldHaveNullData() {
        Result<Void> r = R.ok();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void r_error_shouldReturnResultWith500Code() {
        Result<Void> r = R.error("操作失败");
        assertEquals(500, r.getCode());
        assertEquals("操作失败", r.getMessage());
    }

    @Test
    void r_errorWithCode_shouldUseCustomCode() {
        Result<Void> r = R.error(404, "未找到");
        assertEquals(404, r.getCode());
        assertEquals("未找到", r.getMessage());
    }

    @Test
    void r_withCustomMessage_shouldBeOk() {
        Result<Integer> r = R.ok(42, "查询成功");
        assertEquals(200, r.getCode());
        assertEquals(42, r.getData());
        assertEquals("查询成功", r.getMessage());
    }

    @Test
    void r_businessMethods_shouldReturnCorrectCodes() {
        assertEquals(400, R.badRequest("参数错误").getCode());
        assertEquals(401, R.unauthorized("未登录").getCode());
        assertEquals(403, R.forbidden("无权限").getCode());
        assertEquals(404, R.notFound("未找到").getCode());
        assertEquals(500, R.serverError("服务器错误").getCode());
    }

    // ========== AjaxResult 测试 (继承 Result<Object>) ==========

    @Test
    void ajaxResult_success_shouldReturn200() {
        AjaxResult r = AjaxResult.success();
        assertEquals(200, r.getCode());
        assertEquals("操作成功", r.getMessage());
    }

    @Test
    void ajaxResult_successWithMessage_shouldSetMessage() {
        AjaxResult r = AjaxResult.success("保存成功");
        assertEquals(200, r.getCode());
        assertEquals("保存成功", r.getMessage());
    }

    @Test
    void ajaxResult_successWithDataAndMessage_shouldSetBoth() {
        AjaxResult r = AjaxResult.success("张三", "查询成功");
        assertEquals(200, r.getCode());
        assertEquals("张三", r.getData());
        assertEquals("查询成功", r.getMessage());
    }

    @Test
    void ajaxResult_error_shouldReturn500() {
        AjaxResult r = AjaxResult.error("失败");
        assertEquals(500, r.getCode());
        assertEquals("失败", r.getMessage());
    }

    @Test
    void ajaxResult_errorWithCode_shouldUseCustomCode() {
        AjaxResult r = AjaxResult.error(403, "禁止访问");
        assertEquals(403, r.getCode());
        assertEquals("禁止访问", r.getMessage());
    }

    // ========== TableDataInfo<T> 测试 (继承 PageResult<T>) ==========

    @Test
    void tableDataInfo_constructorWithRowsAndTotal_shouldInit() {
        List<String> rows = Arrays.asList("a", "b", "c");
        TableDataInfo<String> t = new TableDataInfo<>(rows, 100L);
        assertEquals(rows, t.getRows());
        assertEquals(rows, t.getRecords(), "rows 和 records 应指向同一引用");
        assertEquals(100L, t.getTotal());
    }

    @Test
    void tableDataInfo_setRows_shouldAlsoSetRecords() {
        TableDataInfo<String> t = new TableDataInfo<>();
        List<String> rows = Arrays.asList("x", "y");
        t.setRows(rows);
        assertEquals(rows, t.getRows());
        assertEquals(rows, t.getRecords());
    }

    @Test
    void tableDataInfo_defaultCodeAndMsg_shouldBe200() {
        TableDataInfo<String> t = new TableDataInfo<>();
        assertEquals(200, t.getCode());
        assertEquals("查询成功", t.getMsg());
    }

    @Test
    void tableDataInfo_setCodeAndMsg_shouldReturnThis() {
        TableDataInfo<String> t = new TableDataInfo<>();
        TableDataInfo<String> result = t.setCode(500).setMsg("异常");
        assertSame(t, result, "setter 应支持链式调用");
        assertEquals(500, t.getCode());
        assertEquals("异常", t.getMsg());
    }

    @Test
    void tableDataInfo_isAssignableToPageResult() {
        // 验证 csyh 风格兼容: TableDataInfo<VO> 可赋值给 PageResult<VO>
        TableDataInfo<String> t = new TableDataInfo<>();
        PageResult<String> page = t;
        assertSame(t, page);
    }
}
