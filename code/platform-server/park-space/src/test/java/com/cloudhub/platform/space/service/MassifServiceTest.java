package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Massif;
import com.cloudhub.platform.space.mapper.MassifMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * {@link MassifService} 单元测试 (W3.5 阶段)
 */
@ExtendWith(MockitoExtension.class)
class MassifServiceTest {

    @Mock
    private MassifMapper massifMapper;

    @InjectMocks
    private MassifService massifService;

    private Massif massifA;

    @BeforeEach
    void setUp() {
        massifA = makeMassif(1L, "M001", "地块一");
    }

    private Massif makeMassif(Long id, String code, String name) {
        Massif m = new Massif();
        m.setId(id);
        m.setParkId(1L);
        m.setMassifCode(code);
        m.setMassifName(name);
        m.setMassifArea(new BigDecimal("5000.00"));
        m.setUseYear(40);
        m.setLandNatureId(1L);
        m.setPlanUseId(1L);
        m.setAssetType("国土资源");
        m.setStatus(1);
        m.setTenantId(1L);
        return m;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Massif> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(massifA));
        p.setTotal(1);
        when(massifMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Massif>> result = massifService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(massifMapper.selectById(1L)).thenReturn(massifA);
        Result<Massif> result = massifService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("M001", result.getData().getMassifCode());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(massifMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> massifService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("massifCode", "M002");
        params.put("massifName", "地块二");
        params.put("massifArea", "3000.00");
        params.put("landNatureId", 1L);
        params.put("planUseId", 1L);

        when(massifMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Massif m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        }).when(massifMapper).insert(any(Massif.class));

        Result<Long> result = massifService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Massif> captor = ArgumentCaptor.forClass(Massif.class);
        verify(massifMapper).insert(captor.capture());
        Massif inserted = captor.getValue();
        assertEquals("M002", inserted.getMassifCode());
    }

    @Test
    void create_duplicateCode_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("massifCode", "M001");

        when(massifMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> massifService.create(params));
        assertTrue(ex.getMessage().contains("已存在地块编号"));
        verify(massifMapper, never()).insert(any(Massif.class));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(massifMapper.selectById(1L)).thenReturn(massifA);

        Map<String, Object> params = new HashMap<>();
        params.put("massifArea", "6000.00");

        Result<Void> result = massifService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(massifMapper).updateById(any(Massif.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(massifMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> massifService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(massifMapper.selectById(1L)).thenReturn(massifA);

        Result<Void> result = massifService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Massif> captor = ArgumentCaptor.forClass(Massif.class);
        verify(massifMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(massifMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> massifService.delete(999L));
    }
}