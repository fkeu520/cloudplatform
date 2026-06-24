package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Equipment;
import com.cloudhub.platform.space.mapper.EquipmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * {@link EquipmentService} 单元测试 (W3.4 阶段)
 */
@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentMapper equipmentMapper;

    @InjectMocks
    private EquipmentService equipmentService;

    private Equipment equipA;

    @BeforeEach
    void setUp() {
        equipA = makeEquipment(1L, "空调", 10L);
    }

    private Equipment makeEquipment(Long id, String name, Long kitId) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setParkId(1L);
        e.setEquipmentName(name);
        e.setModel("KFR-35");
        e.setAmount(2);
        e.setKitId(kitId);
        e.setStatus(1);
        e.setTenantId(1L);
        return e;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Equipment> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(equipA));
        p.setTotal(1);
        when(equipmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Equipment>> result = equipmentService.page(null, null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(equipmentMapper.selectById(1L)).thenReturn(equipA);
        Result<Equipment> result = equipmentService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("空调", result.getData().getEquipmentName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(equipmentMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> equipmentService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("equipmentName", "投影仪");
        params.put("kitId", 10L);
        params.put("model", "PT-X400");
        params.put("amount", 1);

        when(equipmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Equipment e = inv.getArgument(0);
            e.setId(100L);
            return 1;
        }).when(equipmentMapper).insert(any(Equipment.class));

        Result<Long> result = equipmentService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentMapper).insert(captor.capture());
        Equipment inserted = captor.getValue();
        assertEquals("投影仪", inserted.getEquipmentName());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("equipmentName", "空调");
        params.put("kitId", 10L);

        when(equipmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> equipmentService.create(params));
        assertTrue(ex.getMessage().contains("已存在设备"));
        verify(equipmentMapper, never()).insert(any(Equipment.class));
    }

    @Test
    void create_missingName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        BizException ex = assertThrows(BizException.class, () -> equipmentService.create(params));
        assertTrue(ex.getMessage().contains("缺少必填字段"));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(equipmentMapper.selectById(1L)).thenReturn(equipA);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", 5);

        Result<Void> result = equipmentService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(equipmentMapper).updateById(any(Equipment.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(equipmentMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> equipmentService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(equipmentMapper.selectById(1L)).thenReturn(equipA);

        Result<Void> result = equipmentService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(equipmentMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> equipmentService.delete(999L));
    }

    // ========== BatchSave ==========

    @Test
    void batchSave_duplicateNameInList_shouldThrow() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> a = new HashMap<>();
        a.put("equipmentName", "空调");
        list.add(a);
        Map<String, Object> b = new HashMap<>();
        b.put("equipmentName", "空调");
        list.add(b);

        BizException ex = assertThrows(BizException.class, () -> equipmentService.batchSave(1L, list));
        assertTrue(ex.getMessage().contains("同名设备"));
    }

    @Test
    void batchSave_duplicateNameInDb_shouldThrow() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> a = new HashMap<>();
        a.put("equipmentName", "空调");
        list.add(a);

        when(equipmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> equipmentService.batchSave(1L, list));
        assertTrue(ex.getMessage().contains("同名设备"));
    }
}