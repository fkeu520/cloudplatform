package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Building;
import com.cloudhub.platform.space.mapper.BuildingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * {@link BuildingService} 单元测试 — 重点验证 P0#1 bug 修复:
 * update()/create() 必须保存前端发送的所有 csyh 字段 (areaId, buildingCode,
 * floorNumber, underground, areaCovered, propertyRight, buildingSafety,
 * shareArea, leaseMethod, sorting, certificate, image), 不能静默丢弃.
 */
@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingMapper buildingMapper;

    @InjectMocks
    private BuildingService buildingService;

    private Building existing;

    @BeforeEach
    void setUp() {
        existing = makeBuilding(1L, 100L, 200L, "A 座", "A", 10, 0, new BigDecimal("5000.00"));
    }

    private Building makeBuilding(Long id, Long parkId, Long areaId, String name, String no,
                                  int floorNumber, int underground, BigDecimal areaCovered) {
        Building b = new Building();
        b.setId(id);
        b.setParkId(parkId);
        b.setAreaId(areaId);
        b.setBuildingCode(no);
        b.setBuildingNo(no);
        b.setBuildingName(name);
        b.setFloorNumber(floorNumber);
        b.setUnderground(underground);
        b.setFloors(floorNumber + underground);
        b.setAreaCovered(areaCovered);
        b.setTotalArea(areaCovered);
        b.setStatus(1);
        b.setTenantId(1L);
        return b;
    }

    // ========== Update — P0#1 bug fix: 11 个 csyh 字段不能丢 ==========

    @Test
    void update_allCsyhFields_shouldPersistAll() {
        when(buildingMapper.selectById(1L)).thenReturn(existing);
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 100L);
        params.put("areaId", 201L);                          // ❌ BUG: 原版丢失
        params.put("buildingCode", "A-NEW");                  // ❌ BUG: 原版丢失
        params.put("buildingNo", "A-NEW");
        params.put("buildingName", "A 座 改造");
        params.put("floorNumber", 15);                        // ❌ BUG: 原版丢失
        params.put("underground", 2);                         // ❌ BUG: 原版丢失
        params.put("floors", 17);
        params.put("areaCovered", "8000.50");                 // ❌ BUG: 原版只认 totalArea
        params.put("propertyRight", 0);                      // ❌ BUG: 原版丢失
        params.put("buildingSafety", 1);                     // ❌ BUG: 原版丢失
        params.put("shareArea", "1200.30");                   // ❌ BUG: 原版丢失
        params.put("leaseMethod", 1);                        // ❌ BUG: 原版丢失
        params.put("sorting", 5);                            // ❌ BUG: 原版丢失
        params.put("certificate", "京房权证 X 字第 12345 号");  // ❌ BUG: 原版丢失
        params.put("image", "[\"a.jpg\",\"b.jpg\"]");         // ❌ BUG: 原版丢失
        params.put("buildYear", 2025);
        params.put("manager", "王经理");
        params.put("managerPhone", "13900000001");
        params.put("remark", "改造");
        params.put("status", 1);

        Result<Void> result = buildingService.update(1L, params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingMapper).updateById(captor.capture());
        Building saved = captor.getValue();

        // 关键断言: 每个 csyh 字段都必须被保存
        assertEquals(Long.valueOf(201L), saved.getAreaId(), "areaId 必须保存");
        assertEquals("A-NEW", saved.getBuildingCode(), "buildingCode 必须保存");
        assertEquals(15, saved.getFloorNumber(), "floorNumber 必须保存");
        assertEquals(2, saved.getUnderground(), "underground 必须保存");
        assertEquals(new BigDecimal("8000.50"), saved.getAreaCovered(), "areaCovered 必须保存");
        assertEquals(Integer.valueOf(0), saved.getPropertyRight(), "propertyRight 必须保存");
        assertEquals(Integer.valueOf(1), saved.getBuildingSafety(), "buildingSafety 必须保存");
        assertEquals(new BigDecimal("1200.30"), saved.getShareArea(), "shareArea 必须保存");
        assertEquals(Integer.valueOf(1), saved.getLeaseMethod(), "leaseMethod 必须保存");
        assertEquals(Integer.valueOf(5), saved.getSorting(), "sorting 必须保存");
        assertEquals("京房权证 X 字第 12345 号", saved.getCertificate(), "certificate 必须保存");
        assertEquals("[\"a.jpg\",\"b.jpg\"]", saved.getImage(), "image 必须保存");
    }

    @Test
    void update_partialFieldsOnly_shouldNotOverwriteOthers() {
        // 只更新 areaId + buildingName, 其余字段保持原值
        when(buildingMapper.selectById(1L)).thenReturn(existing);

        Map<String, Object> params = new HashMap<>();
        params.put("areaId", 999L);
        params.put("buildingName", "新名字");

        Result<Void> result = buildingService.update(1L, params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingMapper).updateById(captor.capture());
        Building saved = captor.getValue();

        assertEquals(Long.valueOf(999L), saved.getAreaId(), "新 areaId 必须保存");
        assertEquals("新名字", saved.getBuildingName(), "新 name 必须保存");
        // 未提供的字段保持原值
        assertEquals(Integer.valueOf(10), saved.getFloorNumber(), "未传的 floorNumber 应保留原值");
        assertEquals(new BigDecimal("5000.00"), saved.getAreaCovered(), "未传的 areaCovered 应保留原值");
    }

    @Test
    void update_nullAreaCovered_shouldNotThrow() {
        when(buildingMapper.selectById(1L)).thenReturn(existing);

        Map<String, Object> params = new HashMap<>();
        params.put("areaCovered", null);  // 显式 null

        Result<Void> result = buildingService.update(1L, params);

        assertEquals(200, result.getCode());
        // 显式 null 不应清空原值 (因为 containsKey 为 true 但值为 null, 原版会跳过处理)
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("5000.00"), captor.getValue().getAreaCovered());
    }

    @Test
    void update_emptyParams_shouldNotChangeAnything() {
        when(buildingMapper.selectById(1L)).thenReturn(existing);

        Map<String, Object> params = new HashMap<>();

        Result<Void> result = buildingService.update(1L, params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingMapper).updateById(captor.capture());
        Building saved = captor.getValue();
        // 任何字段都不应被改
        assertEquals(Long.valueOf(200L), saved.getAreaId());
        assertEquals("A 座", saved.getBuildingName());
    }

    @Test
    void update_notFound_shouldThrow() {
        when(buildingMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> buildingService.update(999L, new HashMap<>()));
        verify(buildingMapper, never()).updateById(any(Building.class));
    }

    // ========== Create — 同样 bug, 必须补齐 ==========

    @Test
    void create_allCsyhFields_shouldPersistAll() {
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Building b = inv.getArgument(0);
            b.setId(500L);
            return 1;
        }).when(buildingMapper).insert(any(Building.class));

        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 100L);
        params.put("areaId", 300L);
        params.put("buildingCode", "B");
        params.put("buildingNo", "B");
        params.put("buildingName", "B 座");
        params.put("floorNumber", 8);
        params.put("underground", 1);
        params.put("floors", 9);
        params.put("areaCovered", "3500.75");
        params.put("propertyRight", 1);
        params.put("buildingSafety", 2);
        params.put("shareArea", "500.00");
        params.put("leaseMethod", 0);
        params.put("sorting", 10);
        params.put("certificate", "证 999");
        params.put("image", "[\"x.png\"]");
        params.put("buildYear", 2020);
        params.put("status", 1);

        Result<Long> result = buildingService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingMapper).insert(captor.capture());
        Building inserted = captor.getValue();

        assertEquals(Long.valueOf(100L), inserted.getParkId());
        assertEquals(Long.valueOf(300L), inserted.getAreaId(), "create 也要保存 areaId");
        assertEquals("B", inserted.getBuildingCode());
        assertEquals(8, inserted.getFloorNumber());
        assertEquals(1, inserted.getUnderground());
        assertEquals(new BigDecimal("3500.75"), inserted.getAreaCovered());
        assertEquals(Integer.valueOf(1), inserted.getPropertyRight());
        assertEquals(new BigDecimal("500.00"), inserted.getShareArea());
        assertEquals("证 999", inserted.getCertificate());
        assertEquals("[\"x.png\"]", inserted.getImage());
    }

    @Test
    void create_missingRequiredFields_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("buildingNo", "X");
        // 缺 parkId
        assertThrows(BizException.class, () -> buildingService.create(params));
    }
}
