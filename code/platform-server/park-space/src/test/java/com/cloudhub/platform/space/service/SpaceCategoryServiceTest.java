package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.SpaceCategory;
import com.cloudhub.platform.space.mapper.SpaceCategoryMapper;
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
 * {@link SpaceCategoryService} 单元测试 (W3.5 阶段)
 * <p>V42: parkId 移除后同步更新 (全局唯一名称校验).</p>
 */
@ExtendWith(MockitoExtension.class)
class SpaceCategoryServiceTest {

    @Mock
    private SpaceCategoryMapper spaceCategoryMapper;

    @InjectMocks
    private SpaceCategoryService spaceCategoryService;

    private SpaceCategory catRD;

    @BeforeEach
    void setUp() {
        catRD = makeCategory(1L, "研发中心");
    }

    private SpaceCategory makeCategory(Long id, String name) {
        SpaceCategory sc = new SpaceCategory();
        sc.setId(id);
        sc.setTypeName(name);
        sc.setTypeDescribe("研发空间");
        sc.setStatus(1);
        sc.setTenantId(1L);
        return sc;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<SpaceCategory> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(catRD));
        p.setTotal(1);
        when(spaceCategoryMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<SpaceCategory>> result = spaceCategoryService.page(null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(spaceCategoryMapper.selectById(1L)).thenReturn(catRD);
        Result<SpaceCategory> result = spaceCategoryService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("研发中心", result.getData().getTypeName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(spaceCategoryMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> spaceCategoryService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("typeName", "营销中心");

        when(spaceCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            SpaceCategory sc = inv.getArgument(0);
            sc.setId(100L);
            return 1;
        }).when(spaceCategoryMapper).insert(any(SpaceCategory.class));

        Result<Long> result = spaceCategoryService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<SpaceCategory> captor = ArgumentCaptor.forClass(SpaceCategory.class);
        verify(spaceCategoryMapper).insert(captor.capture());
        SpaceCategory inserted = captor.getValue();
        assertEquals("营销中心", inserted.getTypeName());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("typeName", "研发中心");

        when(spaceCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> spaceCategoryService.create(params));
        assertTrue(ex.getMessage().contains("已存在空间类别"));
        verify(spaceCategoryMapper, never()).insert(any(SpaceCategory.class));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(spaceCategoryMapper.selectById(1L)).thenReturn(catRD);

        Map<String, Object> params = new HashMap<>();
        params.put("typeDescribe", "新描述");

        Result<Void> result = spaceCategoryService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(spaceCategoryMapper).updateById(any(SpaceCategory.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(spaceCategoryMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> spaceCategoryService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(spaceCategoryMapper.selectById(1L)).thenReturn(catRD);

        Result<Void> result = spaceCategoryService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<SpaceCategory> captor = ArgumentCaptor.forClass(SpaceCategory.class);
        verify(spaceCategoryMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(spaceCategoryMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> spaceCategoryService.delete(999L));
    }
}
