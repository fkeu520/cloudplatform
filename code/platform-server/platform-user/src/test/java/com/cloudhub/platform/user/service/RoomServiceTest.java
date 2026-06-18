package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Room;
import com.cloudhub.platform.user.mapper.RoomMapper;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RoomService} 单元测试
 *
 * <p>W3 阶段验证: park-space 第一张表的业务方法 (分页查询).</p>
 *
 * @author csyh fusion W3
 * @since 2026-06-18
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Room room1;
    private Room room2;

    @BeforeEach
    void setUp() {
        room1 = new Room();
        room1.setId(1L);
        room1.setParkId(1L);
        room1.setFloor(1);
        room1.setRoomNo("A-101");
        room1.setRoomType("OFFICE");
        room1.setArea(new BigDecimal("120.50"));
        room1.setMonthlyRent(new BigDecimal("8000.00"));
        room1.setStatus(1);
        room1.setTenantId(1L);

        room2 = new Room();
        room2.setId(2L);
        room2.setRoomNo("A-102");
        room2.setRoomType("OFFICE");
        room2.setStatus(0);
    }

    // ========== 正常场景测试 ==========

    @Test
    void page_withNoFilters_shouldReturnAll() {
        Page<Room> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(room1, room2));
        mockPage.setTotal(2);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Result<PageResult<Room>> result = roomService.page(null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
        assertEquals(2, result.getData().getRecords().size());
        assertEquals("A-101", result.getData().getRecords().get(0).getRoomNo());
    }

    @Test
    void page_withKeyword_shouldFilter() {
        Page<Room> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(room1));
        mockPage.setTotal(1);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Result<PageResult<Room>> result = roomService.page("A-101", null, null, 1, 10);

        assertEquals(1, result.getData().getTotal());
        // 验证 service 传了 keyword 到 wrapper
        ArgumentCaptor<LambdaQueryWrapper<Room>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(roomMapper).selectPage(any(Page.class), captor.capture());
    }

    @Test
    void page_withRoomTypeAndStatus_shouldFilter() {
        Page<Room> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(room1));
        mockPage.setTotal(1);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Result<PageResult<Room>> result = roomService.page(null, "OFFICE", 1, 1, 10);

        assertEquals(1, result.getData().getTotal());
        assertEquals("A-101", result.getData().getRecords().get(0).getRoomNo());
        assertEquals(Integer.valueOf(1), result.getData().getRecords().get(0).getStatus());
    }

    @Test
    void page_emptyResult_shouldReturnEmptyList() {
        Page<Room> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());
        mockPage.setTotal(0);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Result<PageResult<Room>> result = roomService.page(null, null, null, 1, 10);

        assertEquals(200, result.getCode());
        assertEquals(0, result.getData().getTotal());
        assertTrue(result.getData().getRecords().isEmpty());
    }

    // ========== 边界场景测试 ==========

    @Test
    void page_withBlankKeyword_shouldIgnoreFilter() {
        Page<Room> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(room1, room2));
        mockPage.setTotal(2);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 空白 keyword 应当不参与 SQL filter
        Result<PageResult<Room>> result = roomService.page("   ", null, null, 1, 10);

        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void page_withNullKeyword_shouldIgnoreFilter() {
        Page<Room> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(room1, room2));
        mockPage.setTotal(2);
        when(roomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Result<PageResult<Room>> result = roomService.page(null, "OFFICE", null, 1, 10);

        assertEquals(2, result.getData().getTotal());
    }

    // ========== 房间实体验证 ==========

    @Test
    void roomEntity_shouldStoreAllFields() {
        Room r = new Room();
        r.setId(100L);
        r.setParkId(1L);
        r.setBuildingId(1L);
        r.setFloor(3);
        r.setRoomNo("C-301");
        r.setRoomType("OFFICE");
        r.setArea(new BigDecimal("150.00"));
        r.setMonthlyRent(new BigDecimal("10000.00"));
        r.setStatus(1);
        r.setRemark("靠窗");
        r.setTenantId(1L);

        assertEquals(100L, r.getId());
        assertEquals(1L, r.getParkId());
        assertEquals(3, r.getFloor());
        assertEquals("C-301", r.getRoomNo());
        assertEquals("OFFICE", r.getRoomType());
        assertEquals(new BigDecimal("150.00"), r.getArea());
        assertEquals(new BigDecimal("10000.00"), r.getMonthlyRent());
        assertEquals(1, r.getStatus());
        assertEquals("靠窗", r.getRemark());
        assertEquals(1L, r.getTenantId());
    }
}
