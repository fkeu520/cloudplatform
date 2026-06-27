package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.space.domain.dto.RoomMergeDTO;
import com.cloudhub.platform.space.domain.dto.RoomSplitDTO;
import com.cloudhub.platform.space.domain.dto.SplitRoomItem;
import com.cloudhub.platform.space.domain.entity.Room;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import com.cloudhub.platform.space.mapper.RoomMapper;
import com.cloudhub.platform.space.mapper.RoomSplitMergeMapper;
import com.cloudhub.platform.space.remote.UserRemoteClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomSplitMergeServiceTest {

    @Mock
    private RoomSplitMergeMapper roomSplitMergeMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private RoomService roomService;
    @Mock
    private UserRemoteClient userRemoteClient;

    @InjectMocks
    private RoomSplitMergeService roomSplitMergeService;

    private MockedStatic<LoginContextHolder> loginContextHolderMock;

    private RoomSplitMerge splitRecord;

    @BeforeEach
    void setUp() {
        splitRecord = new RoomSplitMerge();
        splitRecord.setId(1L);
        splitRecord.setParkId(1L);
        splitRecord.setUserId(1L);
        splitRecord.setUserName("admin");
        splitRecord.setReasons("拆分");
        splitRecord.setType(1);
        splitRecord.setOldRoomId("100");
        splitRecord.setOldRoomName("A-101");
        splitRecord.setNewRoomId("200");
        splitRecord.setNewRoomName("A-101-a");
        splitRecord.setNum(2);
        splitRecord.setIsExtend(0);
        splitRecord.setTenantId(1L);

        // Mock LoginContextHolder 静态方法 (默认 null, 不影响现有测试)
        loginContextHolderMock = mockStatic(LoginContextHolder.class);
        loginContextHolderMock.when(LoginContextHolder::getUserId).thenReturn(1L);
        loginContextHolderMock.when(LoginContextHolder::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        if (loginContextHolderMock != null) loginContextHolderMock.close();
    }

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<RoomSplitMerge> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(splitRecord));
        p.setTotal(1);
        when(roomSplitMergeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<RoomSplitMerge>> result = roomSplitMergeService.page(null, null, 1, 10);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(roomSplitMergeMapper.selectById(1L)).thenReturn(splitRecord);
        Result<RoomSplitMerge> result = roomSplitMergeService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("100", result.getData().getOldRoomId());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(roomSplitMergeMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> roomSplitMergeService.getById(999L));
    }

    @Test
    void merge_withLessThan2Rooms_shouldThrow() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setOldRoomIds(Collections.singletonList(1L));
        assertThrows(BizException.class, () -> roomSplitMergeService.merge(dto));
    }

    @Test
    void merge_duplicateRoomNo_shouldThrow() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setRoomNo("A-201");
        dto.setRoomName("合并房间");
        dto.setOldRoomIds(List.of(1L, 2L));

        Room old1 = new Room(); old1.setId(1L); old1.setRoomName("A-101"); old1.setStatus(0);
        Room old2 = new Room(); old2.setId(2L); old2.setRoomName("A-102"); old2.setStatus(0);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BizException.class, () -> roomSplitMergeService.merge(dto));
    }

    @Test
    void split_withLessThan2Rooms_shouldThrow() {
        RoomSplitDTO dto = new RoomSplitDTO();
        dto.setRoomList(Collections.singletonList(new SplitRoomItem()));
        assertThrows(BizException.class, () -> roomSplitMergeService.split(dto));
    }

    @Test
    void merge_valid_shouldSucceed() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setRoomNo("A-201");
        dto.setRoomName("合并房间");
        dto.setRoomType("OFFICE");
        dto.setOldRoomIds(List.of(1L, 2L));
        dto.setReasons("合并测试");

        Room old1 = new Room(); old1.setId(1L); old1.setRoomName("A-101"); old1.setStatus(0);
        Room old2 = new Room(); old2.setId(2L); old2.setRoomName("A-102"); old2.setStatus(0);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roomMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(old1, old2));
        doNothing().when(roomService).batchSoftDelete(List.of(1L, 2L));
        when(roomService.insertAndGetId(any(Room.class))).thenReturn(100L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        Result<Room> result = roomSplitMergeService.merge(dto);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void split_valid_shouldSucceed() {
        RoomSplitDTO dto = new RoomSplitDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setOldRoomId(1L);
        dto.setReasons("拆分测试");
        dto.setNum(2);

        SplitRoomItem item1 = new SplitRoomItem();
        item1.setRoomNo("B-101"); item1.setRoomName("B-101-name"); item1.setRoomType("OFFICE");
        SplitRoomItem item2 = new SplitRoomItem();
        item2.setRoomNo("B-102"); item2.setRoomName("B-102-name"); item2.setRoomType("OFFICE");
        dto.setRoomList(List.of(item1, item2));

        Room oldRoom = new Room(); oldRoom.setId(1L); oldRoom.setRoomName("A-101");
        oldRoom.setStatus(0); oldRoom.setRoomType("OFFICE"); oldRoom.setFloor(1);

        when(roomMapper.selectById(1L)).thenReturn(oldRoom);
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(roomService).batchSoftDelete(List.of(1L));
        when(roomService.insertAndGetId(any(Room.class)))
                .thenReturn(200L).thenReturn(201L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        Result<List<Room>> result = roomSplitMergeService.split(dto);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }

    /**
     * Bug 回归测试: split 创建多个新房间时, new_room_id 应为逗号分隔的 String (V51 修复路径)
     * <p>之前 DB 列是 bigint, 存逗号分隔字符串会触发 "Data truncated for column 'new_room_id'" 错误.
     * V51 migration 将列类型改为 varchar(500) 后, 此场景必须可写入.
     */
    @Test
    void split_multiRoom_newRoomIdShouldBeCommaJoinedString() {
        RoomSplitDTO dto = new RoomSplitDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setOldRoomId(1L);
        dto.setReasons("多房间拆分");
        dto.setNum(3);

        // 3 个新房间, area 验证末房抹平尾差 (csyh 移植 + 用户要求)
        SplitRoomItem item1 = new SplitRoomItem();
        item1.setRoomNo("B-101"); item1.setRoomName("B-101-name");
        item1.setAreaCovered(new java.math.BigDecimal("33.33"));
        item1.setBuildArea(new java.math.BigDecimal("25.00"));
        SplitRoomItem item2 = new SplitRoomItem();
        item2.setRoomNo("B-102"); item2.setRoomName("B-102-name");
        item2.setAreaCovered(new java.math.BigDecimal("33.33"));
        item2.setBuildArea(new java.math.BigDecimal("25.00"));
        SplitRoomItem item3 = new SplitRoomItem();
        item3.setRoomNo("B-103"); item3.setRoomName("B-103-name");
        // 末房抹平: 100 - 33.33 * 2 = 33.34 (前端已计算)
        item3.setAreaCovered(new java.math.BigDecimal("33.34"));
        item3.setBuildArea(new java.math.BigDecimal("25.00"));
        dto.setRoomList(List.of(item1, item2, item3));

        Room oldRoom = new Room(); oldRoom.setId(1L); oldRoom.setRoomName("A-101");
        oldRoom.setStatus(0); oldRoom.setRoomType("OFFICE"); oldRoom.setFloor(1);

        when(roomMapper.selectById(1L)).thenReturn(oldRoom);
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(roomService).batchSoftDelete(List.of(1L));
        when(roomService.insertAndGetId(any(Room.class)))
                .thenReturn(200L).thenReturn(201L).thenReturn(202L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        Result<List<Room>> result = roomSplitMergeService.split(dto);

        assertEquals(200, result.getCode());
        assertEquals(3, result.getData().size());

        // 核心断言: mapper.insert 收到的 record.newRoomId 必须是逗号分隔字符串
        org.mockito.ArgumentCaptor<RoomSplitMerge> captor =
                org.mockito.ArgumentCaptor.forClass(RoomSplitMerge.class);
        org.mockito.Mockito.verify(roomSplitMergeMapper).insert(captor.capture());
        RoomSplitMerge saved = captor.getValue();
        assertEquals("200,201,202", saved.getNewRoomId(),
                "split 多房间应拼接逗号分隔的 newRoomId (V51 修复)");
        assertEquals("B-101-name,B-102-name,B-103-name", saved.getNewRoomName());
        assertEquals("A-101", saved.getOldRoomName());
        assertEquals(3, saved.getNum());
        assertEquals(1, saved.getType(), "split 记录 type 应为 1 (拆分)");
    }

    /**
     * 合并时 area 字段从前端传入 (含自动 sum + 用户修改)
     */
    @Test
    void merge_areaOverrideFromFrontend_shouldPersist() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setRoomNo("A-201");
        dto.setRoomName("合并房间");
        dto.setRoomType("OFFICE");
        // 用户在合并 dialog 中修改了面积 (前端预填 sum, 后端仅持久化)
        dto.setAreaCovered(new java.math.BigDecimal("123.45"));
        dto.setBuildArea(new java.math.BigDecimal("98.76"));
        dto.setOldRoomIds(List.of(1L, 2L));

        Room old1 = new Room(); old1.setId(1L); old1.setRoomName("A-101");
        old1.setAreaCovered(new java.math.BigDecimal("60.00"));
        old1.setBuildArea(new java.math.BigDecimal("48.00"));
        old1.setStatus(0);
        Room old2 = new Room(); old2.setId(2L); old2.setRoomName("A-102");
        old2.setAreaCovered(new java.math.BigDecimal("60.00"));
        old2.setBuildArea(new java.math.BigDecimal("48.00"));
        old2.setStatus(0);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roomMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(old1, old2));
        doNothing().when(roomService).batchSoftDelete(List.of(1L, 2L));
        when(roomService.insertAndGetId(any(Room.class))).thenReturn(100L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        Result<Room> result = roomSplitMergeService.merge(dto);

        assertEquals(200, result.getCode());
        assertEquals(new java.math.BigDecimal("123.45"), result.getData().getAreaCovered(),
                "合并后面积 = 前端传入值 (sum 123.45, 不应回退为 sum of old)");
        assertEquals(new java.math.BigDecimal("98.76"), result.getData().getBuildArea());

        // 记录: oldRoomId 应为 "1,2"
        org.mockito.ArgumentCaptor<RoomSplitMerge> captor =
                org.mockito.ArgumentCaptor.forClass(RoomSplitMerge.class);
        org.mockito.Mockito.verify(roomSplitMergeMapper).insert(captor.capture());
        RoomSplitMerge saved = captor.getValue();
        assertEquals("1,2", saved.getOldRoomId(),
                "merge 多房间 oldRoomId 应为逗号分隔 (V51 修复)");
        assertEquals("100", saved.getNewRoomId(), "merge 单房间 newRoomId 为单值字符串");
        assertEquals(0, saved.getType(), "merge 记录 type 应为 0 (合并)");
    }

    @Test
    void restoreMerge_valid_shouldSucceed() {
        RoomSplitMerge record = new RoomSplitMerge();
        record.setId(1L);
        record.setType(0);
        record.setOldRoomId("10,11");
        record.setOldRoomName("A-101,A-102");
        record.setNewRoomId("100");
        record.setNewRoomName("合并房间");
        record.setParkId(1L);
        record.setTenantId(1L);

        Room mergedRoom = new Room(); mergedRoom.setId(100L); mergedRoom.setStatus(0);
        Room old1 = new Room(); old1.setId(10L); old1.setDeleted(1); old1.setStatus(0);
        Room old2 = new Room(); old2.setId(11L); old2.setDeleted(1); old2.setStatus(0);

        when(roomSplitMergeMapper.listByNewRoomId("100")).thenReturn(List.of(record));
        when(roomMapper.selectById(100L)).thenReturn(mergedRoom);
        when(roomMapper.selectById(10L)).thenReturn(old1);
        when(roomMapper.selectById(11L)).thenReturn(old2);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        Result<RoomSplitMerge> result = roomSplitMergeService.restore(100L, 0);
        assertEquals(200, result.getCode());
    }

    @Test
    void restoreSplit_valid_shouldSucceed() {
        RoomSplitMerge record = new RoomSplitMerge();
        record.setId(1L);
        record.setType(1);
        record.setOldRoomId("10");
        record.setOldRoomName("A-101");
        record.setNewRoomId("100,101");
        record.setNewRoomName("B-101,B-102");
        record.setParkId(1L);
        record.setTenantId(1L);

        Room split1 = new Room(); split1.setId(100L); split1.setRoomName("B-101"); split1.setStatus(0);
        Room split2 = new Room(); split2.setId(101L); split2.setRoomName("B-102"); split2.setStatus(0);
        Room oldRoom = new Room(); oldRoom.setId(10L); oldRoom.setDeleted(1); oldRoom.setStatus(0);

        when(roomSplitMergeMapper.getByNewRoomId("100")).thenReturn(record);
        when(roomMapper.selectById(100L)).thenReturn(split1);
        when(roomMapper.selectById(101L)).thenReturn(split2);
        when(roomMapper.selectById(10L)).thenReturn(oldRoom);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        Result<RoomSplitMerge> result = roomSplitMergeService.restore(100L, 1);
        assertEquals(200, result.getCode());
    }

    @Test
    void restoreMerge_roomInUse_shouldThrow() {
        RoomSplitMerge record = new RoomSplitMerge();
        record.setType(0);
        record.setOldRoomId("10,11");
        record.setNewRoomId("100");

        Room mergedRoom = new Room(); mergedRoom.setId(100L); mergedRoom.setStatus(1);

        when(roomSplitMergeMapper.listByNewRoomId("100")).thenReturn(List.of(record));
        when(roomMapper.selectById(100L)).thenReturn(mergedRoom);
        when(roomService.isRoomInUse(mergedRoom)).thenReturn(true);

        assertThrows(BizException.class, () -> roomSplitMergeService.restore(100L, 0));
    }

    // ========== areaId 继承测试 (Bug 2 回归) ==========

    @Test
    void merge_shouldInheritAreaIdFromFirstOldRoom() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setRoomNo("A-201");
        dto.setRoomName("合并房间");
        dto.setRoomType("OFFICE");
        dto.setAreaCovered(new java.math.BigDecimal("100"));
        dto.setBuildArea(new java.math.BigDecimal("80"));
        dto.setOldRoomIds(List.of(1L, 2L));
        dto.setReasons("合并测试");

        Room old1 = new Room(); old1.setId(1L); old1.setRoomName("A-101");
        old1.setStatus(0); old1.setAreaId(10L);
        old1.setRentingSelling(0); old1.setLeasePrice(new java.math.BigDecimal("2"));
        old1.setSalePrice(new java.math.BigDecimal("10000"));
        Room old2 = new Room(); old2.setId(2L); old2.setRoomName("A-102");
        old2.setStatus(0); old2.setAreaId(10L);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roomMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(old1, old2));
        doNothing().when(roomService).batchSoftDelete(List.of(1L, 2L));
        when(roomService.insertAndGetId(any(Room.class))).thenReturn(100L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        roomSplitMergeService.merge(dto);

        // 断言 insertAndGetId 收到的 Room 继承了 areaId
        org.mockito.ArgumentCaptor<Room> roomCaptor =
                org.mockito.ArgumentCaptor.forClass(Room.class);
        verify(roomService).insertAndGetId(roomCaptor.capture());
        Room newRoom = roomCaptor.getValue();
        assertEquals(10L, newRoom.getAreaId(), "合并后的房间应继承第一个旧房间的 areaId");
        // 继承更多字段
        assertEquals(Integer.valueOf(0), newRoom.getRentingSelling(), "合并应继承 rentingSelling");
        assertEquals(new java.math.BigDecimal("2"), newRoom.getLeasePrice(), "合并应继承 leasePrice");
        assertEquals(new java.math.BigDecimal("10000"), newRoom.getSalePrice(), "合并应继承 salePrice");
    }

    @Test
    void split_shouldInheritAreaIdAndExtraFieldsFromOldRoom() {
        RoomSplitDTO dto = new RoomSplitDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setOldRoomId(1L);
        dto.setNum(2);
        dto.setReasons("拆分继承测试");

        SplitRoomItem item1 = new SplitRoomItem();
        item1.setRoomNo("B-101"); item1.setRoomName("B-101-name");
        item1.setAreaCovered(new java.math.BigDecimal("50"));
        SplitRoomItem item2 = new SplitRoomItem();
        item2.setRoomNo("B-102"); item2.setRoomName("B-102-name");
        item2.setAreaCovered(new java.math.BigDecimal("50"));
        dto.setRoomList(List.of(item1, item2));

        Room oldRoom = new Room(); oldRoom.setId(1L); oldRoom.setRoomName("A-101");
        oldRoom.setStatus(0); oldRoom.setRoomType("OFFICE"); oldRoom.setFloor(1);
        oldRoom.setAreaId(10L);
        oldRoom.setKitId(100L); oldRoom.setPurposeId(200L);
        oldRoom.setHouseStructure(1);
        oldRoom.setRentingSelling(2); oldRoom.setLeasePrice(new java.math.BigDecimal("3.5"));
        oldRoom.setSalePrice(new java.math.BigDecimal("20000"));

        when(roomMapper.selectById(1L)).thenReturn(oldRoom);
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(roomService).batchSoftDelete(List.of(1L));
        when(roomService.insertAndGetId(any(Room.class)))
                .thenReturn(200L).thenReturn(201L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        roomSplitMergeService.split(dto);

        // 断言 insertAndGetId 两次调用都收到继承的字段
        org.mockito.ArgumentCaptor<Room> roomCaptor =
                org.mockito.ArgumentCaptor.forClass(Room.class);
        verify(roomService, times(2)).insertAndGetId(roomCaptor.capture());
        List<Room> createdRooms = roomCaptor.getAllValues();
        for (Room r : createdRooms) {
            assertEquals(10L, r.getAreaId(), "拆分出的每个房间都应继承 areaId");
            assertEquals(Long.valueOf(100L), r.getKitId(), "拆分应继承 kitId");
            assertEquals(Long.valueOf(200L), r.getPurposeId(), "拆分应继承 purposeId");
            assertEquals(Integer.valueOf(1), r.getHouseStructure(), "拆分应继承 houseStructure");
            assertEquals(Integer.valueOf(2), r.getRentingSelling(), "拆分应继承 rentingSelling");
            assertEquals(new java.math.BigDecimal("3.5"), r.getLeasePrice(), "拆分应继承 leasePrice");
            assertEquals(new java.math.BigDecimal("20000"), r.getSalePrice(), "拆分应继承 salePrice");
        }
    }

    // ========== type/status 双写测试 (Bug 1 回归) ==========

    @Test
    void mergeRecord_shouldSyncStatusWithType() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setRoomNo("A-201");
        dto.setRoomName("合并房间");
        dto.setRoomType("OFFICE");
        dto.setOldRoomIds(List.of(1L, 2L));

        Room old1 = new Room(); old1.setId(1L); old1.setRoomName("A-101"); old1.setStatus(0);
        Room old2 = new Room(); old2.setId(2L); old2.setRoomName("A-102"); old2.setStatus(0);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roomMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(old1, old2));
        doNothing().when(roomService).batchSoftDelete(List.of(1L, 2L));
        when(roomService.insertAndGetId(any(Room.class))).thenReturn(100L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        roomSplitMergeService.merge(dto);

        org.mockito.ArgumentCaptor<RoomSplitMerge> captor =
                org.mockito.ArgumentCaptor.forClass(RoomSplitMerge.class);
        verify(roomSplitMergeMapper).insert(captor.capture());
        RoomSplitMerge saved = captor.getValue();
        assertEquals(0, saved.getType(), "merge 记录 type=0");
        assertEquals(Integer.valueOf(0), saved.getStatus(), "merge 记录 status 应与 type 同步为 0");
    }

    @Test
    void splitRecord_shouldSyncStatusWithType() {
        RoomSplitDTO dto = new RoomSplitDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setOldRoomId(1L);
        dto.setNum(2);

        SplitRoomItem item1 = new SplitRoomItem();
        item1.setRoomNo("B-101"); item1.setRoomName("B-101-name");
        SplitRoomItem item2 = new SplitRoomItem();
        item2.setRoomNo("B-102"); item2.setRoomName("B-102-name");
        dto.setRoomList(List.of(item1, item2));

        Room oldRoom = new Room(); oldRoom.setId(1L); oldRoom.setRoomName("A-101");
        oldRoom.setStatus(0); oldRoom.setRoomType("OFFICE"); oldRoom.setFloor(1);

        when(roomMapper.selectById(1L)).thenReturn(oldRoom);
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(roomService).batchSoftDelete(List.of(1L));
        when(roomService.insertAndGetId(any(Room.class)))
                .thenReturn(200L).thenReturn(201L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        roomSplitMergeService.split(dto);

        org.mockito.ArgumentCaptor<RoomSplitMerge> captor =
                org.mockito.ArgumentCaptor.forClass(RoomSplitMerge.class);
        verify(roomSplitMergeMapper).insert(captor.capture());
        RoomSplitMerge saved = captor.getValue();
        assertEquals(1, saved.getType(), "split 记录 type=1");
        assertEquals(Integer.valueOf(1), saved.getStatus(), "split 记录 status 应与 type 同步为 1");
    }

    @Test
    void restoreRecord_shouldSyncStatusWithType() {
        // ---- restoreMerge ----
        RoomSplitMerge mergeRec = new RoomSplitMerge();
        mergeRec.setId(1L); mergeRec.setType(0);
        mergeRec.setOldRoomId("10,11"); mergeRec.setOldRoomName("A-101,A-102");
        mergeRec.setNewRoomId("100"); mergeRec.setNewRoomName("合并房间");
        mergeRec.setParkId(1L); mergeRec.setTenantId(1L);

        Room mergedRoom = new Room(); mergedRoom.setId(100L); mergedRoom.setStatus(0);
        Room old1 = new Room(); old1.setId(10L); old1.setDeleted(1); old1.setStatus(0);
        Room old2 = new Room(); old2.setId(11L); old2.setDeleted(1); old2.setStatus(0);

        when(roomSplitMergeMapper.listByNewRoomId("100")).thenReturn(List.of(mergeRec));
        when(roomMapper.selectById(100L)).thenReturn(mergedRoom);
        when(roomMapper.selectById(10L)).thenReturn(old1);
        when(roomMapper.selectById(11L)).thenReturn(old2);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        roomSplitMergeService.restore(100L, 0);

        org.mockito.ArgumentCaptor<RoomSplitMerge> captor =
                org.mockito.ArgumentCaptor.forClass(RoomSplitMerge.class);
        verify(roomSplitMergeMapper, atLeastOnce()).insert(captor.capture());
        List<RoomSplitMerge> saved = captor.getAllValues();
        // restoreMerge 只 insert 1 次
        assertEquals(2, saved.get(0).getType(), "还原记录 type=2");
        assertEquals(Integer.valueOf(2), saved.get(0).getStatus(), "还原记录 status 应与 type 同步为 2");
    }

    @Test
    void merge_mergedRoom_shouldThrow() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setRoomNo("A-201");
        dto.setRoomName("合并房间");
        dto.setOldRoomIds(List.of(1L, 2L));

        Room old1 = new Room(); old1.setId(1L); old1.setRoomName("A-101"); old1.setStatus(1);
        Room old2 = new Room(); old2.setId(2L); old2.setRoomName("A-102"); old2.setStatus(0);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roomMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(old1, old2));
        when(roomService.isRoomInUse(old1)).thenReturn(true);

        assertThrows(BizException.class, () -> roomSplitMergeService.merge(dto));
    }

    // ========== 操作人写入 (userId/userName) ==========

    @Test
    void merge_shouldWriteCurrentUserIdAndUsername() {
        RoomMergeDTO dto = new RoomMergeDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setRoomNo("A-201");
        dto.setRoomName("合并房间");
        dto.setRoomType("OFFICE");
        dto.setOldRoomIds(List.of(1L, 2L));

        Room old1 = new Room(); old1.setId(1L); old1.setRoomName("A-101"); old1.setStatus(0);
        Room old2 = new Room(); old2.setId(2L); old2.setRoomName("A-102"); old2.setStatus(0);

        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roomMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(old1, old2));
        doNothing().when(roomService).batchSoftDelete(List.of(1L, 2L));
        when(roomService.insertAndGetId(any(Room.class))).thenReturn(100L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        roomSplitMergeService.merge(dto);

        org.mockito.ArgumentCaptor<RoomSplitMerge> captor =
                org.mockito.ArgumentCaptor.forClass(RoomSplitMerge.class);
        verify(roomSplitMergeMapper).insert(captor.capture());
        RoomSplitMerge saved = captor.getValue();
        assertEquals(Long.valueOf(1L), saved.getUserId(), "merge 应写当前用户 ID");
        assertEquals("admin", saved.getUserName(), "merge 应写当前用户登录账号");
    }

    @Test
    void split_shouldWriteCurrentUserIdAndUsername() {
        RoomSplitDTO dto = new RoomSplitDTO();
        dto.setParkId(1L);
        dto.setBuildingId(1L);
        dto.setOldRoomId(1L);
        dto.setNum(2);

        SplitRoomItem item1 = new SplitRoomItem();
        item1.setRoomNo("B-101"); item1.setRoomName("B-101-name");
        SplitRoomItem item2 = new SplitRoomItem();
        item2.setRoomNo("B-102"); item2.setRoomName("B-102-name");
        dto.setRoomList(List.of(item1, item2));

        Room oldRoom = new Room(); oldRoom.setId(1L); oldRoom.setRoomName("A-101");
        oldRoom.setStatus(0); oldRoom.setRoomType("OFFICE"); oldRoom.setFloor(1);

        when(roomMapper.selectById(1L)).thenReturn(oldRoom);
        when(roomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(roomService).batchSoftDelete(List.of(1L));
        when(roomService.insertAndGetId(any(Room.class)))
                .thenReturn(200L).thenReturn(201L);
        when(roomSplitMergeMapper.insert(any(RoomSplitMerge.class))).thenReturn(1);

        roomSplitMergeService.split(dto);

        org.mockito.ArgumentCaptor<RoomSplitMerge> captor =
                org.mockito.ArgumentCaptor.forClass(RoomSplitMerge.class);
        verify(roomSplitMergeMapper).insert(captor.capture());
        RoomSplitMerge saved = captor.getValue();
        assertEquals(Long.valueOf(1L), saved.getUserId(), "split 应写当前用户 ID");
        assertEquals("admin", saved.getUserName(), "split 应写当前用户登录账号");
    }

    // ========== 昵称补全 (query-time enrichment) ==========

    @Test
    void page_shouldEnrichUsernameToNickname() {
        RoomSplitMerge rec1 = new RoomSplitMerge();
        rec1.setId(1L); rec1.setUserName("admin"); rec1.setType(0);
        RoomSplitMerge rec2 = new RoomSplitMerge();
        rec2.setId(2L); rec2.setUserName("manager"); rec2.setType(1);

        Page<RoomSplitMerge> p = new Page<>(1, 10);
        p.setRecords(List.of(rec1, rec2));
        p.setTotal(2);
        when(roomSplitMergeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);
        when(userRemoteClient.getNickname("admin")).thenReturn("管理员");
        when(userRemoteClient.getNickname("manager")).thenReturn("经理");

        Result<PageResult<RoomSplitMerge>> result = roomSplitMergeService.page(null, null, 1, 10);
        assertEquals(200, result.getCode());
        List<RoomSplitMerge> records = result.getData().getRecords();
        assertEquals("管理员", records.get(0).getUserName(), "page 记录 1 应被补全为昵称");
        assertEquals("经理", records.get(1).getUserName(), "page 记录 2 应被补全为昵称");
    }

    @Test
    void page_whenNicknameFetchFails_shouldKeepUsername() {
        RoomSplitMerge rec = new RoomSplitMerge();
        rec.setId(1L); rec.setUserName("admin"); rec.setType(0);

        Page<RoomSplitMerge> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(rec));
        p.setTotal(1);
        when(roomSplitMergeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);
        when(userRemoteClient.getNickname("admin")).thenReturn(null); // 模拟失败

        Result<PageResult<RoomSplitMerge>> result = roomSplitMergeService.page(null, null, 1, 10);
        assertEquals("admin", result.getData().getRecords().get(0).getUserName(),
                "昵称获取失败时, 应保留原 username");
    }

    @Test
    void listByRoomId_shouldEnrichNickname() {
        RoomSplitMerge rec = new RoomSplitMerge();
        rec.setId(1L); rec.setUserName("admin"); rec.setType(0);
        when(roomSplitMergeMapper.listByRoomId("100")).thenReturn(List.of(rec));
        when(userRemoteClient.getNickname("admin")).thenReturn("管理员");

        Result<List<RoomSplitMerge>> result = roomSplitMergeService.listByRoomId(100L);
        assertEquals("管理员", result.getData().get(0).getUserName());
    }

    @Test
    void getById_shouldEnrichNickname() {
        RoomSplitMerge rec = new RoomSplitMerge();
        rec.setId(1L); rec.setUserName("admin"); rec.setType(0);
        when(roomSplitMergeMapper.selectById(1L)).thenReturn(rec);
        when(userRemoteClient.getNickname("admin")).thenReturn("管理员");

        Result<RoomSplitMerge> result = roomSplitMergeService.getById(1L);
        assertEquals("管理员", result.getData().getUserName());
    }
}
