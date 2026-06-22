package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Floor;
import com.cloudhub.platform.space.mapper.FloorMapper;
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
 * {@link FloorService} 单元测试 (W3.3 阶段)
 */
@ExtendWith(MockitoExtension.class)
class FloorServiceTest {

    @Mock
    private FloorMapper floorMapper;

    @InjectMocks
    private FloorService floorService;

    private Floor floor1;

    @BeforeEach
    void setUp() {
        floor1 = makeFloor(1L, "1 楼");
    }

    private Floor makeFloor(Long id, String name) {
        Floor f = new Floor();
        f.setId(id);
        f.setParkId(1L);
        f.setBuildingId(1L);
        f.setFloorName(name);
        f.setSerialCode(1);
        f.setFloorCategory(0);
        f.setCoefficient(new BigDecimal("1.00"));
        f.setSorting(1);
        f.setStatus(1);
        f.setTenantId(1L);
        return f;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Floor> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(floor1));
        p.setTotal(1);
        when(floorMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Floor>> result = floorService.page(null, null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<Floor> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(floor1));
        p.setTotal(1);
        when(floorMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Floor>> result = floorService.page("   ", null, null, null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(floorMapper.selectById(1L)).thenReturn(floor1);
        Result<Floor> result = floorService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("1 楼", result.getData().getFloorName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(floorMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> floorService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("buildingId", 1L);
        params.put("floorName", "2 楼");
        params.put("serialCode", 2);
        params.put("coefficient", "1.00");

        when(floorMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Floor f = inv.getArgument(0);
            f.setId(200L);
            return 1;
        }).when(floorMapper).insert(any(Floor.class));

        Result<Long> result = floorService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Floor> captor = ArgumentCaptor.forClass(Floor.class);
        verify(floorMapper).insert(captor.capture());
        Floor inserted = captor.getValue();
        assertEquals("2 楼", inserted.getFloorName());
        assertEquals(Long.valueOf(1L), inserted.getBuildingId());
    }

    @Test
    void create_duplicateNameInSameBuilding_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("buildingId", 1L);
        params.put("floorName", "1 楼");

        when(floorMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> floorService.create(params));
        assertTrue(ex.getMessage().contains("已存在楼层"));
        verify(floorMapper, never()).insert(any(Floor.class));
    }

    @Test
    void create_missingBuildingId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("floorName", "2 楼");
        assertThrows(BizException.class, () -> floorService.create(params));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(floorMapper.selectById(1L)).thenReturn(floor1);

        Map<String, Object> params = new HashMap<>();
        params.put("coefficient", "1.50");

        Result<Void> result = floorService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(floorMapper).updateById(any(Floor.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(floorMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> floorService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(floorMapper.selectById(1L)).thenReturn(floor1);

        Result<Void> result = floorService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Floor> captor = ArgumentCaptor.forClass(Floor.class);
        verify(floorMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(floorMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> floorService.delete(999L));
    }
}