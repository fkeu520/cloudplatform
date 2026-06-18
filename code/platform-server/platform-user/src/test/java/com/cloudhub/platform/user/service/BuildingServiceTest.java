package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Building;
import com.cloudhub.platform.user.mapper.BuildingMapper;
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
 * {@link BuildingService} 单元测试
 *
 * <p>W3.2 阶段验证: 完整 CRUD + 业务约束.</p>
 *
 * @author csyh fusion W3.2
 * @since 2026-06-18
 */
@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingMapper buildingMapper;

    @InjectMocks
    private BuildingService buildingService;

    private Building b1;
    private Building b2;

    @BeforeEach
    void setUp() {
        b1 = makeBuilding(1L, "A", 12);
        b2 = makeBuilding(2L, "B", 8);
    }

    private Building makeBuilding(Long id, String no, int floors) {
        Building b = new Building();
        b.setId(id);
        b.setParkId(1L);
        b.setBuildingNo(no);
        b.setBuildingName(no + " 座");
        b.setFloors(floors);
        b.setTotalArea(new BigDecimal("10000.00"));
        b.setBuildYear(2020);
        b.setStatus(1);
        b.setTenantId(1L);
        return b;
    }

    // ========== Query ==========

    @Test
    void page_noFilters_shouldReturnAll() {
        Page<Building> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(b1, b2));
        p.setTotal(2);
        when(buildingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Building>> result = buildingService.page(null, null, 1, 10);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
        assertEquals("A", result.getData().getRecords().get(0).getBuildingNo());
    }

    @Test
    void page_withKeyword_shouldMatch() {
        Page<Building> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(b1));
        p.setTotal(1);
        when(buildingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Building>> result = buildingService.page("A", null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void page_withBlankKeyword_shouldIgnore() {
        Page<Building> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(b1, b2));
        p.setTotal(2);
        when(buildingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Building>> result = buildingService.page("   ", null, 1, 10);
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(buildingMapper.selectById(1L)).thenReturn(b1);
        Result<Building> result = buildingService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("A", result.getData().getBuildingNo());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(buildingMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> buildingService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("buildingNo", "C");
        params.put("buildingName", "C 座 综合楼");
        params.put("floors", 6);
        params.put("totalArea", 8000);

        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Building b = inv.getArgument(0);
            b.setId(3L);
            return 1;
        }).when(buildingMapper).insert(any(Building.class));

        Result<Long> result = buildingService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingMapper).insert(captor.capture());
        Building inserted = captor.getValue();
        assertEquals("C", inserted.getBuildingNo());
        assertEquals(6, inserted.getFloors());
        assertEquals(Integer.valueOf(1), inserted.getStatus(), "默认 status=1 (正常)");
    }

    @Test
    void create_duplicateBuildingNo_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("buildingNo", "A");

        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> buildingService.create(params));
        assertTrue(ex.getMessage().contains("已存在楼宇编号"));
        verify(buildingMapper, never()).insert(any(Building.class));
    }

    @Test
    void create_missingParkId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("buildingNo", "X");

        assertThrows(BizException.class, () -> buildingService.create(params));
    }

    @Test
    void create_missingBuildingNo_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);

        assertThrows(BizException.class, () -> buildingService.create(params));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(buildingMapper.selectById(1L)).thenReturn(b1);
        Map<String, Object> params = new HashMap<>();
        params.put("buildingName", "A 座 (改造)");
        params.put("floors", 15);

        Result<Void> result = buildingService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(buildingMapper).updateById(any(Building.class));
    }

    @Test
    void update_changeBuildingNoConflict_shouldThrow() {
        when(buildingMapper.selectById(1L)).thenReturn(b1);
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        Map<String, Object> params = new HashMap<>();
        params.put("buildingNo", "B");

        BizException ex = assertThrows(BizException.class, () -> buildingService.update(1L, params));
        assertTrue(ex.getMessage().contains("已存在楼宇编号"));
        verify(buildingMapper, never()).updateById(any(Building.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(buildingMapper.selectById(999L)).thenReturn(null);
        Map<String, Object> params = new HashMap<>();
        params.put("buildingName", "test");

        assertThrows(BizException.class, () -> buildingService.update(999L, params));
    }

    // ========== Delete ==========

    @Test
    void delete_active_shouldSoftDelete() {
        when(buildingMapper.selectById(1L)).thenReturn(b1);

        Result<Void> result = buildingService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_disabled_shouldThrow() {
        b1.setStatus(0);
        when(buildingMapper.selectById(1L)).thenReturn(b1);

        BizException ex = assertThrows(BizException.class, () -> buildingService.delete(1L));
        assertTrue(ex.getMessage().contains("已停用的楼宇不可删除"));
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(buildingMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> buildingService.delete(999L));
    }

    // ========== Entity 字段测试 ==========

    @Test
    void entity_shouldStoreAllFields() {
        Building b = new Building();
        b.setId(100L);
        b.setParkId(1L);
        b.setBuildingNo("D");
        b.setBuildingName("D 座");
        b.setFloors(20);
        b.setTotalArea(new BigDecimal("20000.00"));
        b.setBuildYear(2022);
        b.setManager("王经理");
        b.setManagerPhone("13800000099");
        b.setStatus(1);
        b.setRemark("新建");
        b.setTenantId(1L);

        assertEquals(100L, b.getId());
        assertEquals("D", b.getBuildingNo());
        assertEquals(20, b.getFloors());
        assertEquals(new BigDecimal("20000.00"), b.getTotalArea());
        assertEquals(2022, b.getBuildYear());
        assertEquals("王经理", b.getManager());
        assertEquals("13800000099", b.getManagerPhone());
        assertEquals(1, b.getStatus());
    }
}
