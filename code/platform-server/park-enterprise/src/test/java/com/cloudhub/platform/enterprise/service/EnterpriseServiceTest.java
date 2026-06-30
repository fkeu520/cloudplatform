package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.Enterprise;
import com.cloudhub.platform.enterprise.mapper.EnterpriseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EnterpriseService} 单元测试 (park-enterprise Phase 1)
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseServiceTest {

    @Mock
    private EnterpriseMapper enterpriseMapper;

    @InjectMocks
    private EnterpriseService enterpriseService;

    private Enterprise make(Long id, String name, int status) {
        Enterprise e = new Enterprise();
        e.setId(id);
        e.setName(name);
        e.setCreditCode("91110000" + id + "MA0");
        e.setStatus(status);
        e.setTenantId(1L);
        return e;
    }

    @Test
    void page_withKeyword_shouldFilterByName() {
        Page<Enterprise> p = new Page<>(1, 10);
        p.setRecords(java.util.List.of(make(1L, "测试企业", 1)));
        p.setTotal(1);
        when(enterpriseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Enterprise>> result = enterpriseService.page("测试", null, 1, 10);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        assertEquals(1, result.getData().getRecords().size());
        assertEquals("测试企业", result.getData().getRecords().get(0).getName());
    }

    @Test
    void page_noFilters_shouldReturnAll() {
        Page<Enterprise> p = new Page<>(1, 10);
        p.setRecords(java.util.List.of(make(1L, "A", 1), make(2L, "B", 1)));
        p.setTotal(2);
        when(enterpriseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Enterprise>> result = enterpriseService.page(null, null, 1, 10);

        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void detail_found_shouldReturnEntity() {
        when(enterpriseMapper.selectById(1L)).thenReturn(make(1L, "A", 1));

        Result<Enterprise> result = enterpriseService.detail(1L);

        assertEquals(200, result.getCode());
        assertEquals("A", result.getData().getName());
    }

    @Test
    void detail_notFound_shouldThrowBizException() {
        when(enterpriseMapper.selectById(99L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> enterpriseService.detail(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void create_blankName_shouldThrow() {
        Enterprise e = new Enterprise();
        e.setName("");  // blank

        assertThrows(BizException.class, () -> enterpriseService.create(e, "admin"));
    }

    @Test
    void create_valid_shouldInsertWithAutoDefaults() {
        Enterprise e = make(null, "新建企业", 1);
        when(enterpriseMapper.insert(any(Enterprise.class))).thenReturn(1);

        Result<Enterprise> result = enterpriseService.create(e, "admin");

        assertEquals(200, result.getCode());
        ArgumentCaptor<Enterprise> cap = ArgumentCaptor.forClass(Enterprise.class);
        verify(enterpriseMapper).insert(cap.capture());
        Enterprise captured = cap.getValue();
        // service 入库前清 id, 由 MP 雪花 ID 填充 (运行时); Mockito stub 不写回 ref
        assertNotNull(captured);  // 引用已捕获
        // 验证入库前已清 id (service.create 显式 setId(null))
        // 由于 ArgumentCaptor 捕获的是传入引用, e.setId(null) 应该传播
        assertEquals("新建企业", captured.getName());
        assertEquals(1, captured.getStatus()); // 默认启用
        assertEquals(0, captured.getIsSync());
        assertEquals(0, captured.getIsFill());
        assertEquals("admin", captured.getCreateBy());
        assertNotNull(captured.getCreateTime());
    }

    @Test
    void update_idMissing_shouldThrow() {
        Enterprise e = new Enterprise();
        e.setName("X");

        assertThrows(BizException.class, () -> enterpriseService.update(e, "admin"));
        verify(enterpriseMapper, never()).updateById(any());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(enterpriseMapper.selectById(99L)).thenReturn(null);

        assertThrows(BizException.class, () -> enterpriseService.delete(99L));
        verify(enterpriseMapper, never()).deleteById(any());
    }
}
