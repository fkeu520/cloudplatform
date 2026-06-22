package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Kit;
import com.cloudhub.platform.space.mapper.KitMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * {@link KitService} 单元测试 (W3.3 阶段)
 */
@ExtendWith(MockitoExtension.class)
class KitServiceTest {

    @Mock
    private KitMapper kitMapper;

    @InjectMocks
    private KitService kitService;

    private Kit kitA;

    @BeforeEach
    void setUp() {
        kitA = makeKit(1L, "标准装修");
    }

    private Kit makeKit(Long id, String name) {
        Kit k = new Kit();
        k.setId(id);
        k.setParkId(1L);
        k.setKitName(name);
        k.setAmount(1);
        k.setStatus(1);
        k.setTenantId(1L);
        return k;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Kit> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(kitA));
        p.setTotal(1);
        when(kitMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Kit>> result = kitService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<Kit> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(kitA));
        p.setTotal(1);
        when(kitMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Kit>> result = kitService.page("   ", null, null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(kitMapper.selectById(1L)).thenReturn(kitA);
        Result<Kit> result = kitService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("标准装修", result.getData().getKitName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(kitMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> kitService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("kitName", "豪华装修");
        params.put("amount", 1);

        when(kitMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Kit k = inv.getArgument(0);
            k.setId(100L);
            return 1;
        }).when(kitMapper).insert(any(Kit.class));

        Result<Long> result = kitService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Kit> captor = ArgumentCaptor.forClass(Kit.class);
        verify(kitMapper).insert(captor.capture());
        Kit inserted = captor.getValue();
        assertEquals("豪华装修", inserted.getKitName());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("kitName", "标准装修");

        when(kitMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> kitService.create(params));
        assertTrue(ex.getMessage().contains("已存在配套"));
        verify(kitMapper, never()).insert(any(Kit.class));
    }

    @Test
    void create_missingKitName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        BizException ex = assertThrows(BizException.class, () -> kitService.create(params));
        assertTrue(ex.getMessage().contains("缺少必填字段"));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(kitMapper.selectById(1L)).thenReturn(kitA);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", 5);

        Result<Void> result = kitService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(kitMapper).updateById(any(Kit.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(kitMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> kitService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(kitMapper.selectById(1L)).thenReturn(kitA);

        Result<Void> result = kitService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Kit> captor = ArgumentCaptor.forClass(Kit.class);
        verify(kitMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(kitMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> kitService.delete(999L));
    }
}