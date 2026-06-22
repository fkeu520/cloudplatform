package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Space;
import com.cloudhub.platform.space.mapper.SpaceMapper;
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
 * {@link SpaceService} 单元测试 (W3.5 阶段)
 */
@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock
    private SpaceMapper spaceMapper;

    @InjectMocks
    private SpaceService spaceService;

    private Space spaceA;

    @BeforeEach
    void setUp() {
        spaceA = makeSpace(1L, "研发中心");
    }

    private Space makeSpace(Long id, String name) {
        Space s = new Space();
        s.setId(id);
        s.setParkId(1L);
        s.setAreaId(1L);
        s.setCategoryId(1L);
        s.setSpaceName(name);
        s.setSpaceDescribe("位于 A 区 1 楼");
        s.setStatus(1);
        s.setTenantId(1L);
        return s;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Space> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(spaceA));
        p.setTotal(1);
        when(spaceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Space>> result = spaceService.page(null, null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(spaceMapper.selectById(1L)).thenReturn(spaceA);
        Result<Space> result = spaceService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("研发中心", result.getData().getSpaceName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(spaceMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> spaceService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("spaceName", "营销中心");
        params.put("areaId", 2L);
        params.put("categoryId", 1L);
        params.put("spaceDescribe", "位于 B 区 2 楼");

        when(spaceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Space s = inv.getArgument(0);
            s.setId(100L);
            return 1;
        }).when(spaceMapper).insert(any(Space.class));

        Result<Long> result = spaceService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Space> captor = ArgumentCaptor.forClass(Space.class);
        verify(spaceMapper).insert(captor.capture());
        Space inserted = captor.getValue();
        assertEquals("营销中心", inserted.getSpaceName());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("spaceName", "研发中心");

        when(spaceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> spaceService.create(params));
        assertTrue(ex.getMessage().contains("已存在空间"));
        verify(spaceMapper, never()).insert(any(Space.class));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(spaceMapper.selectById(1L)).thenReturn(spaceA);

        Map<String, Object> params = new HashMap<>();
        params.put("spaceDescribe", "新描述");

        Result<Void> result = spaceService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(spaceMapper).updateById(any(Space.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(spaceMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> spaceService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(spaceMapper.selectById(1L)).thenReturn(spaceA);

        Result<Void> result = spaceService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Space> captor = ArgumentCaptor.forClass(Space.class);
        verify(spaceMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(spaceMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> spaceService.delete(999L));
    }
}