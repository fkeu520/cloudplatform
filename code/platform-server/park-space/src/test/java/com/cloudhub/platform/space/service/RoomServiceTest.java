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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RoomService} + {@link RoomStatus} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Room roomVacant;
    private Room roomRented;
    private Room roomSold;
    private Room roomLocked;
    private Room roomBooked;

    @BeforeEach
    void setUp() {
        roomVacant = makeRoom(1L, 0);
        roomRented = makeRoom(2L, 1);
        roomSold = makeRoom(3L, 2);
        roomLocked = makeRoom(4L, 3);
        roomBooked = makeRoom(5L, 4);
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

    // ========== 分页测试 ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Room> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(roomVacant, roomRented));
        p.setTotal(2);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Room>> result = roomService.page(null, null, null, null, null, null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void page_blankKeyword_shouldIgnoreFilter() {
        Page<Room> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(roomVacant));
        p.setTotal(1);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Room>> result = roomService.page("   ", null, null, null, null, null, null, 1, 10);
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
        doAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(roomMapper).insert(any(Room.class));

        Result<Long> result = roomService.create(params);
        assertEquals(200, result.getCode());

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
    void create_with4LevelFields_shouldPersistAll() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("areaId", 200L);
        params.put("buildingId", 300L);
        params.put("floorId", 400L);
        params.put("roomNo", "B-501");
        params.put("roomName", "B 座 501");
        params.put("floor", 5);
        params.put("roomType", "OFFICE");
        params.put("areaCovered", 120);
        params.put("monthlyRent", 8000);
        params.put("remark", "test 4-level");

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(500L);
            return 1;
        }).when(roomMapper).insert(any(Room.class));

        Result<Long> result = roomService.create(params);
        assertEquals(200, result.getCode());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).insert(captor.capture());
        Room inserted = captor.getValue();
        assertEquals(Long.valueOf(1L), inserted.getParkId());
        assertEquals(Long.valueOf(200L), inserted.getAreaId(), "create 必须保存 areaId");
        assertEquals(Long.valueOf(300L), inserted.getBuildingId());
        assertEquals(Long.valueOf(400L), inserted.getFloorId(), "create 必须保存 floorId");
        assertEquals(Integer.valueOf(5), inserted.getFloor());
        assertEquals("B-501", inserted.getRoomNo());
    }

    @Test
    void update_areaIdChange_shouldPersist() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Map<String, Object> params = new HashMap<>();
        params.put("areaId", 999L);

        Result<Void> result = roomService.update(1L, params);
        assertEquals(200, result.getCode());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(captor.capture());
        assertEquals(Long.valueOf(999L), captor.getValue().getAreaId(), "update 必须保存 areaId");
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

    /**
     * 修复回归测试: V37/V49 新增的字段 (buildArea/billableArea/unitPrice/totalPrice/kitId/purposeId/
     * image/introduce/sorting/houseStructure) 在 update() 中必须被处理.
     * <p>历史 bug: 这些字段在编辑时无法保存 (P0修复 2026-06-26).</p>
     */
    @Test
    void update_v37Fields_shouldPersist() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Map<String, Object> params = new HashMap<>();
        params.put("buildArea", 95);
        params.put("billableArea", 100);
        params.put("unitPrice", 50);
        params.put("totalPrice", 5000);
        params.put("kitId", 10L);
        params.put("purposeId", 20L);
        params.put("image", "[\"img1.jpg\",\"img2.jpg\"]");
        params.put("introduce", "测试介绍");
        params.put("sorting", 99);
        params.put("houseStructure", 1);

        Result<Void> result = roomService.update(1L, params);
        assertEquals(200, result.getCode());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(captor.capture());
        Room updated = captor.getValue();
        assertEquals(0, new BigDecimal("95").compareTo(updated.getBuildArea()), "buildArea 必须保存");
        assertEquals(0, new BigDecimal("100").compareTo(updated.getBillableArea()), "billableArea 必须保存");
        assertEquals(0, new BigDecimal("50").compareTo(updated.getUnitPrice()), "unitPrice 必须保存");
        assertEquals(0, new BigDecimal("5000").compareTo(updated.getTotalPrice()), "totalPrice 必须保存");
        assertEquals(Long.valueOf(10L), updated.getKitId(), "kitId (房间配套) 必须保存");
        assertEquals(Long.valueOf(20L), updated.getPurposeId(), "purposeId (房间用途) 必须保存");
        assertEquals("[\"img1.jpg\",\"img2.jpg\"]", updated.getImage(), "image 必须保存");
        assertEquals("测试介绍", updated.getIntroduce(), "introduce 必须保存");
        assertEquals(Integer.valueOf(99), updated.getSorting(), "sorting 必须保存");
        assertEquals(Integer.valueOf(1), updated.getHouseStructure(), "houseStructure 必须保存");
    }

    @Test
    void update_soldStatus_shouldThrow() {
        when(roomMapper.selectById(3L)).thenReturn(roomSold);
        Map<String, Object> params = new HashMap<>();
        params.put("roomNo", "A-999");

        BizException ex = assertThrows(BizException.class, () -> roomService.update(3L, params));
        assertTrue(ex.getMessage().contains("已售状态的房源不可修改"));
    }

    @Test
    void update_changeRoomNoConflict_shouldThrow() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
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

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_sold_shouldThrow() {
        when(roomMapper.selectById(3L)).thenReturn(roomSold);
        BizException ex = assertThrows(BizException.class, () -> roomService.delete(3L));
        assertTrue(ex.getMessage().contains("已售状态的房源不可删除"));
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
    void state_vacantToSold_shouldSucceed() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 2);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_vacantToLocked_shouldSucceed() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 3);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_vacantToBooked_shouldSucceed() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 4);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_rentedToVacant_shouldSucceed() {
        when(roomMapper.selectById(2L)).thenReturn(roomRented);
        Result<Void> result = roomService.updateStatus(2L, 0);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_lockedToVacant_shouldSucceed() {
        when(roomMapper.selectById(4L)).thenReturn(roomLocked);
        Result<Void> result = roomService.updateStatus(4L, 0);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_bookedToRented_shouldSucceed() {
        when(roomMapper.selectById(5L)).thenReturn(roomBooked);
        Result<Void> result = roomService.updateStatus(5L, 1);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_bookedToVacant_shouldSucceed() {
        when(roomMapper.selectById(5L)).thenReturn(roomBooked);
        Result<Void> result = roomService.updateStatus(5L, 0);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_anyToSold_shouldSucceed() {
        when(roomMapper.selectById(2L)).thenReturn(roomRented);
        Result<Void> result = roomService.updateStatus(2L, 2);
        assertEquals(200, result.getCode());
    }

    @Test
    void state_rentedToLocked_shouldFail() {
        when(roomMapper.selectById(2L)).thenReturn(roomRented);
        BizException ex = assertThrows(BizException.class, () -> roomService.updateStatus(2L, 3));
        assertTrue(ex.getMessage().contains("状态非法转换"));
    }

    @Test
    void state_soldIsTerminal_cannotLeave() {
        when(roomMapper.selectById(3L)).thenReturn(roomSold);
        BizException ex = assertThrows(BizException.class, () -> roomService.updateStatus(3L, 0));
        assertTrue(ex.getMessage().contains("状态非法转换"));
    }

    @Test
    void state_sameStateIsNoop() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        Result<Void> result = roomService.updateStatus(1L, 0);
        assertEquals(200, result.getCode());
        verify(roomMapper, never()).updateById(any(Room.class));
    }

    // ========== RoomStatus 枚举测试 ==========

    @Test
    void enum_fromCode_shouldReturnEnum() {
        assertEquals(RoomStatus.VACANT, RoomStatus.fromCode(0));
        assertEquals(RoomStatus.RENTED, RoomStatus.fromCode(1));
        assertEquals(RoomStatus.SOLD, RoomStatus.fromCode(2));
        assertEquals(RoomStatus.LOCKED, RoomStatus.fromCode(3));
        assertEquals(RoomStatus.BOOKED, RoomStatus.fromCode(4));
        assertEquals(RoomStatus.VACANT, RoomStatus.fromCode(null));
    }

    @Test
    void enum_fromCodeInvalid_shouldThrowBizException() {
        // S2-5: 越界值抛 BizException, 由 GlobalExceptionHandler 返回 400 而非 500
        assertThrows(BizException.class, () -> RoomStatus.fromCode(99));
    }

    // ========== S2-5: updateStatus 越界值防护 ==========

    @Test
    void updateStatus_invalidCode_shouldThrowBizException() {
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        assertThrows(BizException.class, () -> roomService.updateStatus(1L, 99));
    }

    // ========== S2-6: update 禁止改 parkId ==========

    @Test
    void update_parkIdChange_shouldReject() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 999L); // 尝试跨园区迁移
        when(roomMapper.selectById(1L)).thenReturn(roomVacant);
        assertThrows(BizException.class, () -> roomService.update(1L, params));
        verify(roomMapper, never()).updateById(any(Room.class));
    }

    // ========== S2-7: create 加 buildingId 唯一性 ==========

    @Test
    void create_withExistingBuildingRoomNo_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("buildingId", 10L);
        params.put("roomNo", "A-101");
        params.put("roomType", "OFFICE");
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThrows(BizException.class, () -> roomService.create(params));
    }

    @Test
    void create_withNoExistingRoomNo_shouldSucceed() {
        Map<String, Object> params = new HashMap<>();
        params.put("parkId", 1L);
        params.put("buildingId", 10L);
        params.put("roomNo", "A-999");
        params.put("roomType", "OFFICE");
        params.put("areaCovered", "100.00");
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 不抛异常即成功
        Result<Long> result = roomService.create(params);
        assertEquals(200, result.getCode());
    }

    @Test
    void enum_canTransition_allowedPaths() {
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.RENTED));
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.SOLD));
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.LOCKED));
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.BOOKED));
        assertTrue(RoomStatus.RENTED.canTransitionTo(RoomStatus.VACANT));
        assertTrue(RoomStatus.LOCKED.canTransitionTo(RoomStatus.VACANT));
        assertTrue(RoomStatus.BOOKED.canTransitionTo(RoomStatus.RENTED));
        assertTrue(RoomStatus.BOOKED.canTransitionTo(RoomStatus.VACANT));
    }

    @Test
    void enum_canTransition_anyToSold() {
        assertTrue(RoomStatus.VACANT.canTransitionTo(RoomStatus.SOLD));
        assertTrue(RoomStatus.RENTED.canTransitionTo(RoomStatus.SOLD));
        assertTrue(RoomStatus.LOCKED.canTransitionTo(RoomStatus.SOLD));
        assertTrue(RoomStatus.BOOKED.canTransitionTo(RoomStatus.SOLD));
    }

    @Test
    void enum_canTransition_soldIsTerminal() {
        for (RoomStatus target : RoomStatus.values()) {
            if (target == RoomStatus.SOLD) continue;
            assertFalse(RoomStatus.SOLD.canTransitionTo(target),
                    "SOLD 不应能转换到 " + target);
        }
    }

    @Test
    void enum_canTransition_illegalPaths() {
        assertFalse(RoomStatus.RENTED.canTransitionTo(RoomStatus.LOCKED));
        assertFalse(RoomStatus.RENTED.canTransitionTo(RoomStatus.BOOKED));
        assertFalse(RoomStatus.SOLD.canTransitionTo(RoomStatus.VACANT));
        assertFalse(RoomStatus.LOCKED.canTransitionTo(RoomStatus.RENTED));
    }

    @Test
    void enum_matches_shouldCheckCode() {
        assertTrue(RoomStatus.VACANT.matches(0));
        assertTrue(RoomStatus.VACANT.matches(Integer.valueOf(0)));
        assertFalse(RoomStatus.VACANT.matches(1));
        assertFalse(RoomStatus.VACANT.matches(null));
    }
}