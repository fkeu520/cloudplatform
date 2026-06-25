package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.space.domain.entity.Room;
import com.cloudhub.platform.space.domain.entity.RoomLockRecord;
import com.cloudhub.platform.space.mapper.RoomLockRecordMapper;
import com.cloudhub.platform.space.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 园区房屋 Service (park-space 业务)
 * <p>W3.1 阶段: 完整 CRUD + 状态机.</p>
 * <p><b>状态机</b> (5 状态, 见 {@link RoomStatus}):
 * <pre>
 *   VACANT (0) ──┬─→ RENTED (1)        (签订合同)
 *                └─→ RENOVATING (2)    (开始装修)
 *   RENTED (1) ────→ VACANT (0)        (退租)
 *   RENOVATING (2) → VACANT (0)        (装修完成)
 *   任意 ──────────→ DISABLED (3)      (管理员停用, 终态)
 * </pre>
 * <p>W4+ 阶段: 状态变更触发事件 (Kafka), 联动合同/账单模块.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;

    // ========== Query ==========

    /**
     * 分页查询房屋列表
     * <p>Phase 6 (csyh std 融合): 新增 parkId/areaId/buildingId/floorId 维度过滤,
     * 用于左侧 4 层树形导航 (园区 → 分区 → 楼栋 → 楼层).</p>
     * <p>V41: 新增 areaId 过滤 (Room 实体加 area_id 字段, 来自 building.area_id).</p>
     *
     * @param parkId      园区 ID (可选)
     * @param areaId      分区 ID (可选, V41)
     * @param buildingId  楼栋 ID (可选)
     * @param floorId     楼层 ID (可选)
     */
    public Result<PageResult<Room>> page(String keyword, String roomType, Integer status,
                                          Long parkId, Long areaId, Long buildingId, Long floorId,
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
        if (parkId != null) {
            w.eq(Room::getParkId, parkId);
        }
        if (areaId != null) {
            w.eq(Room::getAreaId, areaId);
        }
        if (buildingId != null) {
            w.eq(Room::getBuildingId, buildingId);
        }
        if (floorId != null) {
            w.eq(Room::getFloorId, floorId);
        }
        w.eq(Room::getDeleted, 0).orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNo);

        Page<Room> p = roomMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Room> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[RoomService] page keyword={}, type={}, status={}, parkId={}, buildingId={}, floorId={} -> total={}",
                keyword, roomType, status, parkId, buildingId, floorId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Room> getById(Long id) {
        Room r = roomMapper.selectById(id);
        if (r == null) throw new BizException("房源不存在");
        return Result.ok(r);
    }

    // ========== Create ==========

    /**
     * 新增房源
     * @param params 字段: parkId, buildingId, floor, roomNo, roomType, area, monthlyRent, remark
     * @return 新建房源的 ID
     */
    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String roomNo = requiredString(params, "roomNo");
        String roomType = params.get("roomType") != null ? (String) params.get("roomType") : "OFFICE";

        // 1. 校验: 同园区房号唯一
        Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getParkId, parkId)
                .eq(Room::getRoomNo, roomNo)
                .eq(Room::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在房号 " + roomNo);
        }

        Room r = new Room();
        r.setParkId(parkId);
        r.setBuildingId(params.get("buildingId") != null ? ((Number) params.get("buildingId")).longValue() : null);
        r.setFloor(params.get("floor") != null ? ((Number) params.get("floor")).intValue() : null);
        r.setRoomNo(roomNo);
        r.setRoomType(roomType);
        r.setAreaCovered(params.get("areaCovered") != null ? new BigDecimal(params.get("areaCovered").toString()) : null);
        r.setMonthlyRent(params.get("monthlyRent") != null
                ? new BigDecimal(params.get("monthlyRent").toString()) : null);
        r.setRemark((String) params.get("remark"));
        r.setStatus(RoomStatus.VACANT.code);  // 新建默认空置
        r.setTenantId(currentTenantId());

        roomMapper.insert(r);
        log.info("[RoomService] create: id={}, parkId={}, roomNo={}", r.getId(), parkId, roomNo);
        return Result.ok(r.getId());
    }

    // ========== Update ==========

    /**
     * 更新房源字段 (不含状态, 状态走专用 updateStatus)
     */
    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Room r = roomMapper.selectById(id);
        if (r == null) throw new BizException("房源不存在");
        if (r.getDeleted() != null && r.getDeleted() == 1) throw new BizException("房源已删除");
        if (RoomStatus.DISABLED.matches(r.getStatus())) {
            throw new BizException("停用状态的房源不可修改");
        }

        if (params.containsKey("parkId")) r.setParkId(ServiceUtils.toLong(params.get("parkId")));
        if (params.containsKey("buildingId")) r.setBuildingId(ServiceUtils.toLong(params.get("buildingId")));
        if (params.containsKey("floor")) r.setFloor(ServiceUtils.toInt(params.get("floor")));
        if (params.containsKey("roomNo")) r.setRoomNo((String) params.get("roomNo"));
        if (params.containsKey("roomType")) r.setRoomType((String) params.get("roomType"));
        if (params.containsKey("areaCovered") && params.get("areaCovered") != null) r.setAreaCovered(new BigDecimal(params.get("areaCovered").toString()));
        if (params.containsKey("monthlyRent") && params.get("monthlyRent") != null) r.setMonthlyRent(new BigDecimal(params.get("monthlyRent").toString()));
        if (params.containsKey("remark")) r.setRemark((String) params.get("remark"));

        // 房号变更: 重新校验唯一性
        if (params.containsKey("roomNo")) {
            Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                    .eq(Room::getParkId, r.getParkId())
                    .eq(Room::getRoomNo, r.getRoomNo())
                    .ne(Room::getId, id)
                    .eq(Room::getDeleted, 0));
            if (count != null && count > 0) {
                throw new BizException("园区 " + r.getParkId() + " 已存在房号 " + r.getRoomNo());
            }
        }

        roomMapper.updateById(r);
        log.info("[RoomService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Room r = roomMapper.selectById(id);
        if (r == null) throw new BizException("房源不存在");
        if (RoomStatus.RENTED.matches(r.getStatus())) {
            throw new BizException("已租状态的房源不可删除, 请先退租");
        }
        // 软删除: deleted=1
        r.setDeleted(1);
        roomMapper.updateById(r);
        log.info("[RoomService] delete: id={}", id);
        return Result.ok();
    }

    // ========== State Machine ==========

    /**
     * 状态变更 (状态机驱动, 不允许非法转换)
     */
    @Transactional
    public Result<Void> updateStatus(Long id, int newStatus) {
        Room r = roomMapper.selectById(id);
        if (r == null) throw new BizException("房源不存在");

        RoomStatus from = RoomStatus.fromCode(r.getStatus());
        RoomStatus to = RoomStatus.fromCode(newStatus);
        if (from == to) {
            log.info("[RoomService] updateStatus: id={}, no change ({})", id, from);
            return Result.ok();
        }

        if (!from.canTransitionTo(to)) {
            throw new BizException(
                    String.format("状态非法转换: %s → %s (允许: %s)",
                            from, to, from.allowedTransitions()));
        }

        r.setStatus(to.code);
        roomMapper.updateById(r);
        log.info("[RoomService] updateStatus: id={}, {} → {}", id, from, to);
        return Result.ok();
    }

    // ========== Helpers ==========

    private static Long requiredLong(Map<String, Object> params, String key) {
        return ServiceUtils.requiredLong(params, key);
    }

    private static String requiredString(Map<String, Object> params, String key) {
        return ServiceUtils.requiredString(params, key);
    }

    /**
     * 当前租户 ID (委托 ServiceUtils)
     */
    private static Long currentTenantId() {
        return ServiceUtils.currentTenantId();
    }

    /**
     * 获取当前操作人 (优先取 LoginContextHolder, 兜底 "system")
     */
    private String getCurrentOperator() {
        String username = LoginContextHolder.getUsername();
        return username != null ? username : "system";
    }

    /**
     * 校验同园区+楼栋内房号唯一性 (V37 唯一索引 uk_park_building_room_no)
     * @param parkId 园区 ID
     * @param buildingId 楼栋 ID
     * @param roomNo 房号
     * @param excludeId 排除的 ID (编辑时传自身)
     * @return true=房号可用, false=已存在
     */
    public Result<Boolean> checkNo(Long parkId, Long buildingId, String roomNo, Long excludeId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Room> w = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Room>()
                .eq(Room::getParkId, parkId)
                .eq(Room::getBuildingId, buildingId)
                .eq(Room::getRoomNo, roomNo)
                .eq(Room::getDeleted, 0);
        if (excludeId != null) {
            w.ne(Room::getId, excludeId);
        }
        Long count = roomMapper.selectCount(w);
        boolean available = count == null || count == 0;
        log.info("[RoomService] checkNo parkId={}, buildingId={}, roomNo={} -> available={}", parkId, buildingId, roomNo, available);
        return Result.ok(available);
    }

    // ========== 租售控制 ==========

    /**
     * 分页查询房源 (含租售控制筛选)
     */
    public Result<PageResult<Room>> controlPage(Long parkId, Long buildingId, Long floorId,
                                                 Integer rentingSelling, Integer isLock,
                                                 String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<Room> w = new LambdaQueryWrapper<Room>()
                .eq(Room::getDeleted, 0);
        if (parkId != null) w.eq(Room::getParkId, parkId);
        if (buildingId != null) w.eq(Room::getBuildingId, buildingId);
        if (floorId != null) w.eq(Room::getFloorId, floorId);
        if (rentingSelling != null) w.eq(Room::getRentingSelling, rentingSelling);
        if (isLock != null) w.eq(Room::getIsLock, isLock);
        if (keyword != null && !keyword.isBlank()) {
            w.like(Room::getRoomNo, keyword).or(w2 -> w2.like(Room::getRoomName, keyword));
        }
        w.orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNo);

        Page<Room> p = roomMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Room> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        return Result.ok(result);
    }

    /**
     * 单个房间租售控制更新 (只更新租房售相关字段)
     */
    @Transactional
    public Result<Void> updateControl(Long id, Integer rentingSelling,
                                       BigDecimal leasePrice, BigDecimal salePrice,
                                       Integer isOrder) {
        Room r = roomMapper.selectById(id);
        if (r == null) throw new BizException("房源不存在");
        if (r.getDeleted() != null && r.getDeleted() == 1) throw new BizException("房源已删除");

        if (rentingSelling != null) r.setRentingSelling(rentingSelling);
        if (leasePrice != null) r.setLeasePrice(leasePrice);
        if (salePrice != null) r.setSalePrice(salePrice);
        if (isOrder != null) r.setIsOrder(isOrder);
        roomMapper.updateById(r);
        log.info("[RoomService] updateControl: id={}, rentingSelling={}", id, rentingSelling);
        return Result.ok();
    }

    /**
     * 批量更新租售控制 (单条 UPDATE ... WHERE id IN (...))
     */
    @Transactional
    public Result<Void> batchUpdateControl(List<Long> ids, Integer rentingSelling,
                                            BigDecimal leasePrice, BigDecimal salePrice) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要更新的房间");
        }
        Room update = new Room();
        if (rentingSelling != null) update.setRentingSelling(rentingSelling);
        if (leasePrice != null) update.setLeasePrice(leasePrice);
        if (salePrice != null) update.setSalePrice(salePrice);
        roomMapper.update(update, new LambdaUpdateWrapper<Room>()
                .in(Room::getId, ids)
                .eq(Room::getDeleted, 0));
        log.info("[RoomService] batchUpdateControl: ids={}, rentingSelling={}", ids.size(), rentingSelling);
        return Result.ok();
    }

    // ========== 房间锁定/解锁 ==========

    private final RoomLockRecordMapper roomLockRecordMapper;

    /**
     * 锁定房间 (同时写锁定记录)
     * <p>使用 DB 级乐观锁 (UPDATE ... WHERE is_lock=0) 防止并发重复锁定.</p>
     */
    @Transactional
    public Result<Void> lockRoom(Long roomId, Long enterpriseId, String enterpriseName,
                                  String reason, Integer days) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BizException("房源不存在");

        // DB 级乐观锁: 只有 is_lock=0 或 null 时才能更新为 1
        int affected = roomMapper.update(null, new LambdaUpdateWrapper<Room>()
                .set(Room::getIsLock, 1)
                .eq(Room::getId, roomId)
                .and(w -> w.eq(Room::getIsLock, 0).or().isNull(Room::getIsLock)));
        if (affected == 0) {
            throw new BizException("房间已被锁定");
        }

        RoomLockRecord record = new RoomLockRecord();
        record.setRoomId(roomId);
        record.setIsLock(1);
        record.setEnterpriseId(enterpriseId);
        record.setEnterpriseName(enterpriseName);
        record.setOperator(getCurrentOperator());
        record.setReason(reason);
        record.setDays(days);
        record.setParkId(r.getParkId());
        record.setTenantId(currentTenantId());
        roomLockRecordMapper.insert(record);

        log.info("[RoomService] lockRoom: roomId={}, reason={}", roomId, reason);
        return Result.ok();
    }

    /**
     * 解锁房间 (同时写解锁记录)
     * <p>使用 DB 级乐观锁 (UPDATE ... WHERE is_lock=1) 防止并发重复解锁.</p>
     */
    @Transactional
    public Result<Void> unlockRoom(Long roomId, String reason) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BizException("房源不存在");

        // DB 级乐观锁: 只有 is_lock=1 时才能更新为 0
        int affected = roomMapper.update(null, new LambdaUpdateWrapper<Room>()
                .set(Room::getIsLock, 0)
                .eq(Room::getId, roomId)
                .eq(Room::getIsLock, 1));
        if (affected == 0) {
            throw new BizException("房间未锁定");
        }

        RoomLockRecord record = new RoomLockRecord();
        record.setRoomId(roomId);
        record.setIsLock(0);
        record.setOperator(getCurrentOperator());
        record.setReason(reason);
        record.setParkId(r.getParkId());
        record.setTenantId(currentTenantId());
        roomLockRecordMapper.insert(record);

        log.info("[RoomService] unlockRoom: roomId={}, reason={}", roomId, reason);
        return Result.ok();
    }

    // ========== Batch ops for Split/Merge ==========

    /**
     * 批量软删除 (单条 UPDATE ... WHERE id IN (...))
     */
    @Transactional
    public void batchSoftDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        roomMapper.update(null, new LambdaUpdateWrapper<Room>()
                .set(Room::getDeleted, 1)
                .in(Room::getId, ids)
                .eq(Room::getDeleted, 0));
        log.info("[RoomService] batchSoftDelete: ids={}", ids.size());
    }

    public List<Room> listByIds(List<Long> ids) {
        return roomMapper.selectBatchIds(ids);
    }

    @Transactional
    public void insertBatch(List<Room> rooms) {
        for (Room r : rooms) {
            roomMapper.insert(r);
        }
    }

    public boolean isRoomInUse(Room r) {
        return RoomStatus.RENTED.matches(r.getStatus()) || RoomStatus.RENOVATING.matches(r.getStatus());
    }

    @Transactional
    public Long insertAndGetId(Room r) {
        roomMapper.insert(r);
        return r.getId();
    }
}
