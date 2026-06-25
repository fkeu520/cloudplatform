package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Park;
import com.cloudhub.platform.space.mapper.ParkMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkServiceTest {

    @Mock
    private ParkMapper parkMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ParkService parkService;

    private Park parkA;
    private Park parkB;

    @BeforeEach
    void setUp() {
        parkA = makePark(1L, "科技园", 1);
        parkB = makePark(2L, "创业园", 1);
    }

    private Park makePark(Long id, String name, int status) {
        Park p = new Park();
        p.setId(id);
        p.setParkName(name);
        p.setProvince("广东省");
        p.setCity("深圳市");
        p.setDistrict("南山区");
        p.setAddress("科技南路");
        p.setStatus(status);
        p.setDeleted(0);
        p.setTenantId(1L);
        return p;
    }

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Park> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(parkA, parkB));
        p.setTotal(2);
        when(parkMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Park>> result = parkService.page(null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<Park> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(parkA));
        p.setTotal(1);
        when(parkMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Park>> result = parkService.page("   ", null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(parkMapper.selectById(1L)).thenReturn(parkA);
        Result<Park> result = parkService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("科技园", result.getData().getParkName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(parkMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> parkService.getById(999L));
    }

    @Test
    void create_valid_shouldInsert() {
        when(parkMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Park p = inv.getArgument(0);
            p.setId(100L);
            return 1;
        }).when(parkMapper).insert(any(Park.class));

        Park input = new Park();
        input.setParkName("新园区");
        input.setProvince("广东省");

        Result<Long> result = parkService.create(input);
        assertEquals(200, result.getCode());
        assertEquals(100L, result.getData());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        when(parkMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        Park input = new Park();
        input.setParkName("科技园");

        BizException ex = assertThrows(BizException.class, () -> parkService.create(input));
        assertEquals("园区名称已存在: 科技园", ex.getMessage());
        verify(parkMapper, never()).insert(any());
    }

    @Test
    void update_valid_shouldUpdate() {
        when(parkMapper.selectById(1L)).thenReturn(parkA);
        when(parkMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        Park update = new Park();
        update.setParkName("科技园（新）");

        Result<Void> result = parkService.update(1L, update);
        assertEquals(200, result.getCode());
        ArgumentCaptor<Park> captor = ArgumentCaptor.forClass(Park.class);
        verify(parkMapper).updateById(captor.capture());
        assertEquals("科技园（新）", captor.getValue().getParkName());
    }

    @Test
    void update_nameConflict_shouldThrow() {
        when(parkMapper.selectById(1L)).thenReturn(parkA);
        when(parkMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        Park update = new Park();
        update.setParkName("创业园");

        BizException ex = assertThrows(BizException.class, () -> parkService.update(1L, update));
        assertEquals("园区名称已存在: 创业园", ex.getMessage());
        verify(parkMapper, never()).updateById(any());
    }

    @Test
    void update_notFound_shouldThrow() {
        when(parkMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> parkService.update(999L, new Park()));
    }

    @Test
    void delete_existing_shouldSoftDelete() {
        when(parkMapper.selectById(1L)).thenReturn(parkA);

        Result<Void> result = parkService.delete(1L);
        assertEquals(200, result.getCode());
        ArgumentCaptor<Park> captor = ArgumentCaptor.forClass(Park.class);
        verify(parkMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(parkMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> parkService.delete(999L));
    }

    @Test
    void toggleStatus_valid_shouldUpdate() {
        when(parkMapper.selectById(1L)).thenReturn(parkA);

        Result<Void> result = parkService.toggleStatus(1L, 0);
        assertEquals(200, result.getCode());
        ArgumentCaptor<Park> captor = ArgumentCaptor.forClass(Park.class);
        verify(parkMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(0), captor.getValue().getStatus());
    }

    @Test
    void toggleStatus_invalidValue_shouldThrow() {
        assertThrows(BizException.class, () -> parkService.toggleStatus(1L, 99));
    }

    @Test
    void listAll_shouldReturnAllParks() {
        when(parkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(parkA, parkB));

        Result<java.util.List<Park>> result = parkService.listAll();
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }

    @Test
    void listAll_shouldIncludeDisabledParks() {
        Park disabled = makePark(3L, "已停用园区", 0);
        when(parkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(parkA, disabled));

        Result<java.util.List<Park>> result = parkService.listAll();
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals("已停用园区", result.getData().get(1).getParkName());
        assertEquals(0, result.getData().get(1).getStatus());
    }
}
