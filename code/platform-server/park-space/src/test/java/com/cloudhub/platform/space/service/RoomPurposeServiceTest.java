package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomPurpose;
import com.cloudhub.platform.space.mapper.RoomPurposeMapper;
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
 * {@link RoomPurposeService} 单元测试 (W3.6 阶段)
 */
@ExtendWith(MockitoExtension.class)
class RoomPurposeServiceTest {

    @Mock
    private RoomPurposeMapper roomPurposeMapper;

    @InjectMocks
    private RoomPurposeService roomPurposeService;

    private RoomPurpose purposeRD;

    @BeforeEach
    void setUp() {
        purposeRD = makePurpose(1L, "自用");
    }

    private RoomPurpose makePurpose(Long id, String name) {
        RoomPurpose p = new RoomPurpose();
        p.setId(id);
        p.setParkId(1L);
        p.setPurposeName(name);
        p.setStatus(1);
        p.setTenantId(1L);
        return p;
    }

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<RoomPurpose> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(purposeRD));
        p.setTotal(1);
        when(roomPurposeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<RoomPurpose>> result = roomPurposeService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(roomPurposeMapper.selectById(1L)).thenReturn(purposeRD);
        Result<RoomPurpose> result = roomPurposeService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("自用", result.getData().getPurposeName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(roomPurposeMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomPurposeService.getById(999L));
    }

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("purposeName", "出租");

        when(roomPurposeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            RoomPurpose p = inv.getArgument(0);
            p.setId(100L);
            return 1;
        }).when(roomPurposeMapper).insert(any(RoomPurpose.class));

        Result<Long> result = roomPurposeService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<RoomPurpose> captor = ArgumentCaptor.forClass(RoomPurpose.class);
        verify(roomPurposeMapper).insert(captor.capture());
        RoomPurpose inserted = captor.getValue();
        assertEquals("出租", inserted.getPurposeName());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("purposeName", "自用");

        when(roomPurposeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> roomPurposeService.create(params));
        assertTrue(ex.getMessage().contains("已存在用途"));
        verify(roomPurposeMapper, never()).insert(any(RoomPurpose.class));
    }

    @Test
    void delete_existing_shouldSoftDelete() {
        when(roomPurposeMapper.selectById(1L)).thenReturn(purposeRD);

        Result<Void> result = roomPurposeService.delete(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<RoomPurpose> captor = ArgumentCaptor.forClass(RoomPurpose.class);
        verify(roomPurposeMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(roomPurposeMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomPurposeService.delete(999L));
    }
}