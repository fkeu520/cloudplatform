package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Room;
import com.cloudhub.platform.space.mapper.RoomMapper;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RoomService} + {@link RoomStatus} 单元测试
 * <p>W3.1 阶段验证:
 * <ul>
 *   <li>分页查询 (W3 hello world 已覆盖)</li>
 *   <li>新增房源 (含唯一性校验)</li>
 *   <li>更新字段 (含 DISABLED 状态保护)</li>
 *   <li>软删除 (RENTED 状态保护)</li>
 *   <li>状态机 (5 状态 + 4 允许转换 + 1 终态)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Room roomVacant;     // 空置
    private Room roomRented;     // 已租
    private Room roomRenovating; // 装修中
    private Room roomDisabled;   // 停用

    @BeforeEach
    void setUp() {
        roomVacant = makeRoom(1L, 0);     // VACANT
        roomRented = makeRoom(2L, 1);     // RENTED
        roomRenovating = makeRoom(3L, 2); // RENOVATING
        roomDisabled = makeRoom(4L, 3);   // DISABLED
    }

    private Room makeRoom(Long id, int status) {
        Room r = new Room();
        r.setId(id);
        r.setParkId(1L);
        r.setRoomNo("A-10" + id);
        r.setRoomType("OFFICE");
        r.setStatus(status);
        r.setTenantId(1L);
        return r;
    }

    // ========== 分页测试 (W3 hello world 验证) ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Room> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(roomVacant, roomRented));
        p.setTotal(2);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Room>> result = roomService.page(null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<Room> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(roomVacant));
        p.setTotal(1);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Room>> result = roomService.page("   ", null, null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    // ========== 详情测试 ==========

    @Test
    void getById_existing_shouldReturn() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Room> result = roomService.getById(1L);
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(roomMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomService.getById(999L));
    }

    // ========== Create 测试 ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("roomNo", "A-201");
        params.put("roomType", "OFFICE");
        params.put("area", 100);
        params.put("monthlyRent", 5000);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟 insert 后给 Room 设置 ID (MP 真实行为)
        doAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(roomMapper).insert(any(Room.class));

        Result<Long> result = roomService.create(params);

        assertEquals(200, result.getCode());
        // 验证 insert 被调用且 Room 含正确字段
        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).insert(captor.capture());
        Room inserted = captor.getValue();
        assertEquals("A-201", inserted.getRoomNo());
        assertEquals(Integer.valueOf(0), inserted.getStatus(), "新建默认 VACANT 状态");
        assertEquals(Long.valueOf(1L), inserted.getParkId());
    }

    @Test
    void create_duplicateRoomNo_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("roomNo", "A-101");

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> roomService.create(params));
        assertTrue(ex.getMessage().contains("已存在房号"));
        verify(roomMapper, never()).insert(any(Room.class));
    }

    @Test
    void create_missingParkId_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("roomNo", "A-201");

        assertThrows(BizException.class, () -> roomService.create(params));
    }

    @Test
    void create_missingRoomNo_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);

        BizException ex = assertThrows(BizException.class, () -> roomService.create(params));
        assertTrue(ex.getMessage().contains("缺少必填字段"));
    }

    // ========== Update 测试 ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Map<String, Object> params = new HashMap<>();
        params.put("roomNo", "A-999");
        params.put("area", 200);

        Result<Void> result = roomService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(roomMapper).updateById(any(Room.class));
    }

    @Test
    void update_disabledStatus_shouldThrow() {
        when(roomMapper.selectById(4L)).thenReturn(roomDisabled);
        Map<String, Object> params = new HashMap<>();
        params.put("roomNo", "A-999");

        BizException ex = assertThrows(BizException.class, () -> roomService.update(4L, params));
        assertTrue(ex.getMessage().contains("停用状态的房源不可修改"));
    }

    @Test
    void update_changeRoomNoConflict_shouldThrow() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        // 第一次 selectCount (用于唯一性校验) 返回 > 0
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        Map<String, Object> params = new HashMap<>();
        params.put("roomNo", "A-CONFLICT");

        BizException ex = assertThrows(BizException.class, () -> roomService.update(1L, params));
        assertTrue(ex.getMessage().contains("已存在房号"));
        verify(roomMapper, never()).updateById(any(Room.class));
    }

    // ========== Delete 测试 ==========

    @Test
    void delete_vacant_shouldSoftDelete() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);

        Result<Void> result = roomService.delete(1L);

        assertEquals(200, result.getCode());
        // 验证 deleted 字段被设为 1
        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_rented_shouldThrow() {
        when(roomMapper.selectById(2L)).thenReturn(roomRented);

        BizException ex = assertThrows(BizException.class, () -> roomService.delete(2L));
        assertTrue(ex.getMessage().contains("已租状态的房源不可删除"));
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(roomMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomService.delete(999L));
    }

    // ========== State Machine 测试 ==========

    @Test
    void state_vacantToRented_shouldSucceed() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 1);
        assertEquals(200, result.getCode());
        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getStatus());
    }

    @Test
    void state_vacantToRenovating_shouldSucceed() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 2);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_rentedToVacant_shouldSucceed() {
        when(roomMapper.selectById(2L)).thenReturn(roomRented);
        Result<Void> result = roomService.updateStatus(2L, 0);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_renovatingToVacant_shouldSucceed() {
        when(roomMapper.selectById(3L)).thenReturn(roomRenovating);
        Result<Void> result = roomService.updateStatus(3L, 0);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_anyToDisabled_shouldSucceed() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 3);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_vacantToDisabled_shouldSucceed() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 3);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_rentedToRenovating_shouldFail() {
        // 非法: RENTED → RENOVATING (必须先退租到 VACANT)
        when(roomMapper.selectById(2L)).thenReturn(roomRented);
        BizException ex = assertThrows(BizException.class, () -> roomService.updateStatus(2L, 2));
        assertTrue(ex.getMessage().contains("状态非法转换"));
    }

    @Test
    void state_disabledIsTerminal_cannotLeave() {
        when(roomMapper.selectById(4L)).thenReturn(roomDisabled);
        BizException ex = assertThrows(BizException.class, () -> roomService.updateStatus(4L, 0));
        assertTrue(ex.getMessage().contains("状态非法转换"));
    }

    @Test
    void state_sameStateIsNoop() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 0);
        assertEquals(200, result.getCode());
        // 同状态不调用 update
        verify(roomMapper, never()).updateById(any(Room.class));
    }

    // ========== RoomStatus 枚举测试 ==========

    @Test
    void enum_fromCode_shouldReturnEnum() {
        assertEquals(RoomStatus.VACANT, RoomStatus.fromCode(0));
        assertEquals(RoomStatus.RENTED, RoomStatus.fromCode(1));
        assertEquals(RoomStatus.RENOVATING, RoomStatus.fromCode(2));
        assertEquals(RoomStatus.DISABLED, RoomStatus.fromCode(3));
        assertEquals(RoomStatus.VACANT, RoomStatus.fromCode(null));
    }

    @Test
    void enum_fromCodeInvalid_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> RoomStatus.fromCode(99));
    }

    @Test
    void enum_canTransition_allowedPaths() {
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.RENTED));
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.RENOVATING));
        assertTrue(RoomStatus.RENTED.canTransitionTo(RoomStatus.VACANT));
        assertTrue(RoomStatus.RENOVATING.canTransitionTo(RoomStatus.VACANT));
    }

    @Test
    void enum_canTransition_anyToDisabled() {
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.DISABLED));
        assertTrue(RoomStatus.RENTED.canTransitionTo(RoomStatus.DISABLED));
        assertTrue(RoomStatus.RENOVATING.canTransitionTo(RoomStatus.DISABLED));
    }

    @Test
    void enum_canTransition_disabledIsTerminal() {
        for (RoomStatus target : RoomStatus.values()) {
            if (target == RoomStatus.DISABLED) continue;
            assertFalse(RoomStatus.DISABLED.canTransitionTo(target),
                    "DISABLED 不应能转换到 " + target);
        }
    }

    @Test
    void enum_canTransition_illegalPaths() {
        // RENTED 不能直接跳到 RENOVATING
        assertFalse(RoomStatus.RENTED.canTransitionTo(RoomStatus.RENOVATING));
        // RENOVATING 不能直接跳到 RENTED
        assertFalse(RoomStatus.RENOVATING.canTransitionTo(RoomStatus.RENTED));
        // VACANT 不能跳到自己以外的非法状态
        assertFalse(RoomStatus.VACANT.canTransitionTo(RoomStatus.VACANT) == false); // 同状态允许 (noop)
    }

    @Test
    void enum_matches_shouldCheckCode() {
        assertTrue(RoomStatus.VACANT.matches(0));
        assertTrue(RoomStatus.VACANT.matches(Integer.valueOf(0)));
        assertFalse(RoomStatus.VACANT.matches(1));
        assertFalse(RoomStatus.VACANT.matches(null));
    }
}
