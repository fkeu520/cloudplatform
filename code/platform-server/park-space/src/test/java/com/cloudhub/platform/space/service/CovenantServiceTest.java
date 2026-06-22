package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Covenant;
import com.cloudhub.platform.space.mapper.CovenantMapper;
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
 * {@link CovenantService} 单元测试 (W3.4 阶段)
 */
@ExtendWith(MockitoExtension.class)
class CovenantServiceTest {

    @Mock
    private CovenantMapper covenantMapper;

    @InjectMocks
    private CovenantService covenantService;

    private Covenant covenantA;

    @BeforeEach
    void setUp() {
        covenantA = makeCovenant(1L, 100L, 200L);
    }

    private Covenant makeCovenant(Long id, Long roomId, Long covenantId) {
        Covenant c = new Covenant();
        c.setId(id);
        c.setParkId(1L);
        c.setRoomId(roomId);
        c.setCovenantId(covenantId);
        c.setCovenantType(0);
        c.setCustomerId(50L);
        c.setStatus(1);
        c.setTenantId(1L);
        return c;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Covenant> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(covenantA));
        p.setTotal(1);
        when(covenantMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Covenant>> result = covenantService.page(null, null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<Covenant> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(covenantA));
        p.setTotal(1);
        when(covenantMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Covenant>> result = covenantService.page("   ", null, null, null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(covenantMapper.selectById(1L)).thenReturn(covenantA);
        Result<Covenant> result = covenantService.getById(1L);
        assertNotNull(result.getData());
        assertEquals(Long.valueOf(100L), result.getData().getRoomId());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(covenantMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> covenantService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("roomId", 100L);
        params.put("covenantId", 200L);
        params.put("covenantType", 0);

        doAnswer(inv -> {
            Covenant c = inv.getArgument(0);
            c.setId(100L);
            return 1;
        }).when(covenantMapper).insert(any(Covenant.class));

        Result<Long> result = covenantService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Covenant> captor = ArgumentCaptor.forClass(Covenant.class);
        verify(covenantMapper).insert(captor.capture());
        Covenant inserted = captor.getValue();
        assertEquals(Long.valueOf(200L), inserted.getCovenantId());
    }

    @Test
    void create_missingRoomId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("covenantId", 200L);
        assertThrows(BizException.class, () -> covenantService.create(params));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(covenantMapper.selectById(1L)).thenReturn(covenantA);

        Map<String, Object> params = new HashMap<>();
        params.put("status", 0);

        Result<Void> result = covenantService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(covenantMapper).updateById(any(Covenant.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(covenantMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> covenantService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(covenantMapper.selectById(1L)).thenReturn(covenantA);

        Result<Void> result = covenantService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Covenant> captor = ArgumentCaptor.forClass(Covenant.class);
        verify(covenantMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(covenantMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> covenantService.delete(999L));
    }
}