package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Energy;
import com.cloudhub.platform.space.mapper.EnergyMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EnergyService} 单元测试 (W3.4 阶段)
 */
@ExtendWith(MockitoExtension.class)
class EnergyServiceTest {

    @Mock
    private EnergyMapper energyMapper;

    @InjectMocks
    private EnergyService energyService;

    private Energy energyA;

    @BeforeEach
    void setUp() {
        energyA = makeEnergy(1L, 100L, 10L);
    }

    private Energy makeEnergy(Long id, Long roomId, Long meterId) {
        Energy e = new Energy();
        e.setId(id);
        e.setParkId(1L);
        e.setRoomId(roomId);
        e.setMeterId(meterId);
        e.setMeterClassId(1L);
        e.setStatus(1);
        e.setTenantId(1L);
        return e;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Energy> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(energyA));
        p.setTotal(1);
        when(energyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Energy>> result = energyService.page(null, null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(energyMapper.selectById(1L)).thenReturn(energyA);
        Result<Energy> result = energyService.getById(1L);
        assertNotNull(result.getData());
        assertEquals(Long.valueOf(100L), result.getData().getRoomId());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(energyMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> energyService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("roomId", 200L);
        params.put("meterId", 20L);

        doAnswer(inv -> {
            Energy e = inv.getArgument(0);
            e.setId(100L);
            return 1;
        }).when(energyMapper).insert(any(Energy.class));

        Result<Long> result = energyService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Energy> captor = ArgumentCaptor.forClass(Energy.class);
        verify(energyMapper).insert(captor.capture());
        Energy inserted = captor.getValue();
        assertEquals(Long.valueOf(200L), inserted.getRoomId());
        assertEquals(Long.valueOf(20L), inserted.getMeterId());
    }

    @Test
    void create_missingParkId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("roomId", 200L);
        assertThrows(BizException.class, () -> energyService.create(params));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(energyMapper.selectById(1L)).thenReturn(energyA);

        Map<String, Object> params = new HashMap<>();
        params.put("status", 0);

        Result<Void> result = energyService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(energyMapper).updateById(any(Energy.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(energyMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> energyService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(energyMapper.selectById(1L)).thenReturn(energyA);

        Result<Void> result = energyService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Energy> captor = ArgumentCaptor.forClass(Energy.class);
        verify(energyMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(energyMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> energyService.delete(999L));
    }
}