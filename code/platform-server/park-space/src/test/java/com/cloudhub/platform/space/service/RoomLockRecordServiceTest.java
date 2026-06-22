package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomLockRecord;
import com.cloudhub.platform.space.mapper.RoomLockRecordMapper;
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
 * {@link RoomLockRecordService} 单元测试 (W3.6 阶段)
 */
@ExtendWith(MockitoExtension.class)
class RoomLockRecordServiceTest {

    @Mock
    private RoomLockRecordMapper roomLockRecordMapper;

    @InjectMocks
    private RoomLockRecordService roomLockRecordService;

    private RoomLockRecord lockRecord;

    @BeforeEach
    void setUp() {
        lockRecord = makeRecord(1L, 100L, 1);
    }

    private RoomLockRecord makeRecord(Long id, Long roomId, int isLock) {
        RoomLockRecord r = new RoomLockRecord();
        r.setId(id);
        r.setParkId(1L);
        r.setRoomId(roomId);
        r.setIsLock(isLock);
        r.setEnterpriseId(50L);
        r.setEnterpriseName("客户A");
        r.setOperator("admin");
        r.setReason("临时锁定");
        r.setDays(7);
        r.setTenantId(1L);
        return r;
    }

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<RoomLockRecord> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(lockRecord));
        p.setTotal(1);
        when(roomLockRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<RoomLockRecord>> result = roomLockRecordService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(roomLockRecordMapper.selectById(1L)).thenReturn(lockRecord);
        Result<RoomLockRecord> result = roomLockRecordService.getById(1L);
        assertNotNull(result.getData());
        assertEquals(Integer.valueOf(1), result.getData().getIsLock());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(roomLockRecordMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomLockRecordService.getById(999L));
    }

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("roomId", 200L);
        params.put("isLock", 1);
        params.put("operator", "admin");
        params.put("reason", "客户预定");
        params.put("days", 14);

        doAnswer(inv -> {
            RoomLockRecord r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(roomLockRecordMapper).insert(any(RoomLockRecord.class));

        Result<Long> result = roomLockRecordService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<RoomLockRecord> captor = ArgumentCaptor.forClass(RoomLockRecord.class);
        verify(roomLockRecordMapper).insert(captor.capture());
        RoomLockRecord inserted = captor.getValue();
        assertEquals(Long.valueOf(200L), inserted.getRoomId());
        assertEquals(Integer.valueOf(1), inserted.getIsLock());
    }

    @Test
    void create_missingRoomId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        assertThrows(BizException.class, () -> roomLockRecordService.create(params));
    }
}