package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.PlanUse;
import com.cloudhub.platform.space.mapper.PlanUseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PlanUseService} 单元测试 (W3.3 阶段)
 */
@ExtendWith(MockitoExtension.class)
class PlanUseServiceTest {

    @Mock
    private PlanUseMapper planUseMapper;

    @InjectMocks
    private PlanUseService planUseService;

    private PlanUse planUseOffice;

    @BeforeEach
    void setUp() {
        planUseOffice = makePlanUse(1L, "OFFICE", "办公");
    }

    private PlanUse makePlanUse(Long id, String code, String name) {
        PlanUse p = new PlanUse();
        p.setId(id);
        p.setParkId(1L);
        p.setPlanUseCode(code);
        p.setPlanUseName(name);
        p.setColor("#409EFF");
        p.setStatus(1);
        p.setTenantId(1L);
        return p;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<PlanUse> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(planUseOffice));
        p.setTotal(1);
        when(planUseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<PlanUse>> result = planUseService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<PlanUse> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(planUseOffice));
        p.setTotal(1);
        when(planUseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<PlanUse>> result = planUseService.page("   ", null, null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(planUseMapper.selectById(1L)).thenReturn(planUseOffice);
        Result<PlanUse> result = planUseService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("办公", result.getData().getPlanUseName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(planUseMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> planUseService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("planUseCode", "SHOP");
        params.put("planUseName", "商业");
        params.put("color", "#67C23A");

        when(planUseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            PlanUse p = inv.getArgument(0);
            p.setId(100L);
            return 1;
        }).when(planUseMapper).insert(any(PlanUse.class));

        Result<Long> result = planUseService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<PlanUse> captor = ArgumentCaptor.forClass(PlanUse.class);
        verify(planUseMapper).insert(captor.capture());
        PlanUse inserted = captor.getValue();
        assertEquals("商业", inserted.getPlanUseName());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("planUseName", "办公");

        when(planUseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> planUseService.create(params));
        assertTrue(ex.getMessage().contains("已存在规划用途"));
        verify(planUseMapper, never()).insert(any(PlanUse.class));
    }

    @Test
    void create_missingName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        BizException ex = assertThrows(BizException.class, () -> planUseService.create(params));
        assertTrue(ex.getMessage().contains("缺少必填字段"));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(planUseMapper.selectById(1L)).thenReturn(planUseOffice);

        Map<String, Object> params = new HashMap<>();
        params.put("color", "#FF0000");

        Result<Void> result = planUseService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(planUseMapper).updateById(any(PlanUse.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(planUseMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> planUseService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(planUseMapper.selectById(1L)).thenReturn(planUseOffice);

        Result<Void> result = planUseService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<PlanUse> captor = ArgumentCaptor.forClass(PlanUse.class);
        verify(planUseMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(planUseMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> planUseService.delete(999L));
    }
}