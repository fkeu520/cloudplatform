package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.LandNature;
import com.cloudhub.platform.space.mapper.LandNatureMapper;
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
 * {@link LandNatureService} 单元测试 (W3.3 阶段)
 */
@ExtendWith(MockitoExtension.class)
class LandNatureServiceTest {

    @Mock
    private LandNatureMapper landNatureMapper;

    @InjectMocks
    private LandNatureService landNatureService;

    private LandNature landIndustrial;

    @BeforeEach
    void setUp() {
        landIndustrial = makeLandNature(1L, "INDUSTRIAL", "工业用地");
    }

    private LandNature makeLandNature(Long id, String code, String name) {
        LandNature l = new LandNature();
        l.setId(id);
        l.setParkId(1L);
        l.setLandNatureCode(code);
        l.setLandNatureName(name);
        l.setColor("#909399");
        l.setStatus(1);
        l.setTenantId(1L);
        return l;
    }

    // ========== Page ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<LandNature> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(landIndustrial));
        p.setTotal(1);
        when(landNatureMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<LandNature>> result = landNatureService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<LandNature> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(landIndustrial));
        p.setTotal(1);
        when(landNatureMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<LandNature>> result = landNatureService.page("   ", null, null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    // ========== GetById ==========

    @Test
    void getById_existing_shouldReturn() {
        when(landNatureMapper.selectById(1L)).thenReturn(landIndustrial);
        Result<LandNature> result = landNatureService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("工业用地", result.getData().getLandNatureName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(landNatureMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> landNatureService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("landNatureCode", "COMMERCIAL");
        params.put("landNatureName", "商业用地");
        params.put("color", "#F56C6C");

        when(landNatureMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            LandNature l = inv.getArgument(0);
            l.setId(100L);
            return 1;
        }).when(landNatureMapper).insert(any(LandNature.class));

        Result<Long> result = landNatureService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<LandNature> captor = ArgumentCaptor.forClass(LandNature.class);
        verify(landNatureMapper).insert(captor.capture());
        LandNature inserted = captor.getValue();
        assertEquals("商业用地", inserted.getLandNatureName());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("landNatureName", "工业用地");

        when(landNatureMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> landNatureService.create(params));
        assertTrue(ex.getMessage().contains("已存在土地性质"));
        verify(landNatureMapper, never()).insert(any(LandNature.class));
    }

    @Test
    void create_missingName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        BizException ex = assertThrows(BizException.class, () -> landNatureService.create(params));
        assertTrue(ex.getMessage().contains("缺少必填字段"));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(landNatureMapper.selectById(1L)).thenReturn(landIndustrial);

        Map<String, Object> params = new HashMap<>();
        params.put("color", "#FF0000");

        Result<Void> result = landNatureService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(landNatureMapper).updateById(any(LandNature.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(landNatureMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> landNatureService.update(999L, new HashMap<>()));
    }

    // ========== Delete ==========

    @Test
    void delete_existing_shouldSoftDelete() {
        when(landNatureMapper.selectById(1L)).thenReturn(landIndustrial);

        Result<Void> result = landNatureService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<LandNature> captor = ArgumentCaptor.forClass(LandNature.class);
        verify(landNatureMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(landNatureMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> landNatureService.delete(999L));
    }
}