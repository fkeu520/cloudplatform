package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Area;
import com.cloudhub.platform.space.mapper.AreaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
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
 * {@link AreaService} 单元测试 (W3.3 阶段)
 */
@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    private AreaMapper areaMapper;

    @InjectMocks
    private AreaService areaService;

    private Area areaA;
    private Area areaB;

    @BeforeEach
    void setUp() {
        areaA = makeArea(1L, "A 区", 1);
        areaB = makeArea(2L, "B 区", 1);
    }

    private Area makeArea(Long id, String name, int status) {
        Area a = new Area();
        a.setId(id);
        a.setParkId(1L);
        a.setAreaName(name);
        a.setAreaCovered(new BigDecimal("10000.00"));
        a.setBuiltArea(new BigDecimal("8000.00"));
        a.setFunctionArea("研发区");
        a.setBuildingAmount(5);
        a.setRoomAmount(50);
        a.setStatus(status);
        a.setTenantId(1L);
        return a;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Area> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(areaA, areaB));
        p.setTotal(2);
        when(areaMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Area>> result = areaService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<Area> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(areaA));
        p.setTotal(1);
        when(areaMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Area>> result = areaService.page("   ", null, null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(areaMapper.selectById(1L)).thenReturn(areaA);
        Result<Area> result = areaService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("A 区", result.getData().getAreaName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(areaMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> areaService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("areaName", "C 区");
        params.put("areaCovered", "5000.00");

        when(areaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Area a = inv.getArgument(0);
            a.setId(100L);
            return 1;
        }).when(areaMapper).insert(any(Area.class));

        Result<Long> result = areaService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Area> captor = ArgumentCaptor.forClass(Area.class);
        verify(areaMapper).insert(captor.capture());
        Area inserted = captor.getValue();
        assertEquals("C 区", inserted.getAreaName());
        assertEquals(Long.valueOf(1L), inserted.getParkId());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("areaName", "A 区");

        when(areaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> areaService.create(params));
        assertTrue(ex.getMessage().contains("已存在区域"));
        verify(areaMapper, never()).insert(any(Area.class));
    }

    @Test
    void create_missingParkId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("areaName", "C 区");
        assertThrows(BizException.class, () -> areaService.create(params));
    }

    @Test
    void create_missingAreaName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        BizException ex = assertThrows(BizException.class, () -> areaService.create(params));
        assertTrue(ex.getMessage().contains("缺少必填字段"));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(areaMapper.selectById(1L)).thenReturn(areaA);

        Map<String, Object> params = new HashMap<>();
        params.put("areaName", "A 区（新）");

        Result<Void> result = areaService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(areaMapper).updateById(any(Area.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(areaMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> areaService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(areaMapper.selectById(1L)).thenReturn(areaA);

        Result<Void> result = areaService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Area> captor = ArgumentCaptor.forClass(Area.class);
        verify(areaMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(areaMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> areaService.delete(999L));
    }
}