package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import com.cloudhub.platform.space.mapper.RoomSplitMergeMapper;
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
 * {@link RoomSplitMergeService} 单元测试 (W3.6 阶段)
 */
@ExtendWith(MockitoExtension.class)
class RoomSplitMergeServiceTest {

    @Mock
    private RoomSplitMergeMapper roomSplitMergeMapper;

    @InjectMocks
    private RoomSplitMergeService roomSplitMergeService;

    private RoomSplitMerge splitRecord;

    @BeforeEach
    void setUp() {
        splitRecord = makeRecord(1L, 100L, 200L);
    }

    private RoomSplitMerge makeRecord(Long id, Long oldRoomId, Long newRoomId) {
        RoomSplitMerge r = new RoomSplitMerge();
        r.setId(id);
        r.setParkId(1L);
        r.setUserId(1L);
        r.setUserName("admin");
        r.setReasons("拆分");
        r.setType(1);
        r.setOldRoomId(oldRoomId);
        r.setOldRoomName("A-101");
        r.setNewRoomId(newRoomId);
        r.setNewRoomName("A-101-a");
        r.setNum(2);
        r.setIsExtend(0);
        r.setStatus(1);
        r.setTenantId(1L);
        return r;
    }

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<RoomSplitMerge> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(splitRecord));
        p.setTotal(1);
        when(roomSplitMergeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<RoomSplitMerge>> result = roomSplitMergeService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(roomSplitMergeMapper.selectById(1L)).thenReturn(splitRecord);
        Result<RoomSplitMerge> result = roomSplitMergeService.getById(1L);
        assertNotNull(result.getData());
        assertEquals(Long.valueOf(100L), result.getData().getOldRoomId());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(roomSplitMergeMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomSplitMergeService.getById(999L));
    }

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("oldRoomId", 100L);
        params.put("newRoomId", 300L);
        params.put("userName", "admin");
        params.put("status", 0);

        doAnswer(inv -> {
            RoomSplitMerge r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(roomSplitMergeMapper).insert(any(RoomSplitMerge.class));

        Result<Long> result = roomSplitMergeService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<RoomSplitMerge> captor = ArgumentCaptor.forClass(RoomSplitMerge.class);
        verify(roomSplitMergeMapper).insert(captor.capture());
        RoomSplitMerge inserted = captor.getValue();
        assertEquals(Long.valueOf(100L), inserted.getOldRoomId());
        assertEquals(Long.valueOf(300L), inserted.getNewRoomId());
    }

    @Test
    void create_missingParkId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("oldRoomId", 100L);
        assertThrows(BizException.class, () -> roomSplitMergeService.create(params));
    }
}