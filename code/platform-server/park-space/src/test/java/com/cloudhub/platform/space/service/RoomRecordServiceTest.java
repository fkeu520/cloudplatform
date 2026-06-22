package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomRecord;
import com.cloudhub.platform.space.mapper.RoomRecordMapper;
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
 * {@link RoomRecordService} 单元测试 (W3.6 阶段)
 */
@ExtendWith(MockitoExtension.class)
class RoomRecordServiceTest {

    @Mock
    private RoomRecordMapper roomRecordMapper;

    @InjectMocks
    private RoomRecordService roomRecordService;

    private RoomRecord record;

    @BeforeEach
    void setUp() {
        record = makeRecord(1L, 100L);
    }

    private RoomRecord makeRecord(Long id, Long roomId) {
        RoomRecord r = new RoomRecord();
        r.setId(id);
        r.setParkId(1L);
        r.setRoomId(roomId);
        r.setCustomerId(50L);
        r.setCovenantId(1L);
        r.setCovenantType(0);
        r.setStatus(0);
        r.setTenantId(1L);
        return r;
    }

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<RoomRecord> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(record));
        p.setTotal(1);
        when(roomRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<RoomRecord>> result = roomRecordService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(roomRecordMapper.selectById(1L)).thenReturn(record);
        Result<RoomRecord> result = roomRecordService.getById(1L);
        assertNotNull(result.getData());
        assertEquals(Long.valueOf(100L), result.getData().getRoomId());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(roomRecordMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomRecordService.getById(999L));
    }

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("roomId", 200L);
        params.put("customerId", 60L);
        params.put("covenantId", 2L);
        params.put("status", 0);

        doAnswer(inv -> {
            RoomRecord r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(roomRecordMapper).insert(any(RoomRecord.class));

        Result<Long> result = roomRecordService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<RoomRecord> captor = ArgumentCaptor.forClass(RoomRecord.class);
        verify(roomRecordMapper).insert(captor.capture());
        RoomRecord inserted = captor.getValue();
        assertEquals(Long.valueOf(200L), inserted.getRoomId());
        assertEquals(Integer.valueOf(0), inserted.getStatus());
    }

    @Test
    void create_missingRoomId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        assertThrows(BizException.class, () -> roomRecordService.create(params));
    }
}