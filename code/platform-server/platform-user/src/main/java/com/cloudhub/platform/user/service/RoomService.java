package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Room;
import com.cloudhub.platform.user.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 园区房屋 Service (park-space 业务)
 *
 * <p>W3 阶段: 仅分页查询, 为前端"房源列表"页面提供数据.
 * W4+ 阶段: 增删改 + 状态机 + 合同关联.</p>
 *
 * @author csyh fusion W3
 * @since 2026-06-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;

    /**
     * 分页查询房屋列表
     *
     * @param keyword  房号模糊匹配 (可选)
     * @param roomType 房间类型 (OFFICE/MEETING/...) (可选)
     * @param status   状态 (0/1/2/3) (可选)
     * @param pageNum  页码 (从 1 开始)
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Result<PageResult<Room>> page(String keyword, String roomType, Integer status,
                                          int pageNum, int pageSize) {
        LambdaQueryWrapper<Room> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Room::getRoomNo, keyword);
        }
        if (roomType != null && !roomType.isBlank()) {
            w.eq(Room::getRoomType, roomType);
        }
        if (status != null) {
            w.eq(Room::getStatus, status);
        }
        w.eq(Room::getDeleted, 0).orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNo);

        Page<Room> p = roomMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Room> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[RoomService] page keyword={}, type={}, status={} -> total={}",
                keyword, roomType, status, p.getTotal());
        return Result.ok(result);
    }
}
