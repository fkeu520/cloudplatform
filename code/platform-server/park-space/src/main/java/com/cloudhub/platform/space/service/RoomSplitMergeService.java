package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.dto.RoomMergeDTO;
import com.cloudhub.platform.space.domain.dto.RoomSplitDTO;
import com.cloudhub.platform.space.domain.dto.SplitRoomItem;
import com.cloudhub.platform.space.domain.entity.Room;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import com.cloudhub.platform.space.mapper.RoomMapper;
import com.cloudhub.platform.space.mapper.RoomSplitMergeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomSplitMergeService {

    private final RoomSplitMergeMapper roomSplitMergeMapper;
    private final RoomMapper roomMapper;
    private final RoomService roomService;

    private static final String COMMA = ",";

    // ========== Query ==========

    public Result<PageResult<RoomSplitMerge>> page(Long parkId, Integer type,
                                                    int pageNum, int pageSize) {
        LambdaQueryWrapper<RoomSplitMerge> w = new LambdaQueryWrapper<>();
        if (parkId != null) {
            w.eq(RoomSplitMerge::getParkId, parkId);
        }
        if (type != null) {
            w.eq(RoomSplitMerge::getType, type);
        }
        w.eq(RoomSplitMerge::getDeleted, 0).orderByDesc(RoomSplitMerge::getCreateTime);

        Page<RoomSplitMerge> p = roomSplitMergeMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<RoomSplitMerge> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        return Result.ok(result);
    }

    public Result<RoomSplitMerge> getById(Long id) {
        RoomSplitMerge r = roomSplitMergeMapper.selectById(id);
        if (r == null) throw new BizException("拆分合并记录不存在");
        return Result.ok(r);
    }

    public Result<List<RoomSplitMerge>> listByRoomId(Long roomId) {
        List<RoomSplitMerge> list = roomSplitMergeMapper.listByRoomId(String.valueOf(roomId));
        return Result.ok(list);
    }

    // ========== Merge ==========

    @Transactional
    public Result<Room> merge(RoomMergeDTO dto) {
        Long parkId = dto.getParkId();
        Long buildingId = dto.getBuildingId();
        List<Long> oldRoomIds = dto.getOldRoomIds();
        if (oldRoomIds == null || oldRoomIds.size() < 2) {
            throw new BizException("合并至少需要 2 个房间");
        }

        // 检查房间编号唯一性
        if (existRoomNo(parkId, dto.getRoomNo(), buildingId)) {
            throw new BizException("房号 " + dto.getRoomNo() + " 已存在");
        }

        // 检查房间名称唯一性
        if (existRoomName(parkId, dto.getRoomName(), buildingId)) {
            throw new BizException("房间名称 " + dto.getRoomName() + " 已存在");
        }

        // 获取旧房间信息
        List<Room> oldRooms = roomMapper.selectBatchIds(oldRoomIds);
        if (oldRooms.size() != oldRoomIds.size()) {
            throw new BizException("部分旧房间不存在");
        }
        String oldRoomNames = oldRooms.stream().map(Room::getRoomName).collect(Collectors.joining(COMMA));

        // 检查旧房间是否在使用中
        for (Room r : oldRooms) {
            if (roomService.isRoomInUse(r)) {
                throw new BizException("房间 " + r.getRoomName() + " 正在使用中，不可合并");
            }
        }

        // 软删除旧房间
        roomService.batchSoftDelete(oldRoomIds);

        // 创建新房间
        Room newRoom = new Room();
        newRoom.setParkId(parkId);
        newRoom.setBuildingId(buildingId);
        newRoom.setFloor(dto.getFloor());
        newRoom.setFloorId(dto.getFloorId());
        newRoom.setRoomNo(dto.getRoomNo());
        newRoom.setRoomName(dto.getRoomName());
        newRoom.setRoomType(dto.getRoomType() != null ? dto.getRoomType() : "OFFICE");
        newRoom.setAreaCovered(dto.getAreaCovered());
        newRoom.setBuildArea(dto.getBuildArea());
        newRoom.setBillableArea(dto.getBillableArea());
        newRoom.setUnitPrice(dto.getUnitPrice());
        newRoom.setMonthlyRent(dto.getMonthlyRent());
        // 继承更多字段
        Room firstOld = oldRooms.get(0);
        newRoom.setAreaId(firstOld.getAreaId());
        newRoom.setHouseStructure(firstOld.getHouseStructure());
        newRoom.setRentingSelling(firstOld.getRentingSelling());
        newRoom.setUnitPrice(firstOld.getUnitPrice());
        newRoom.setMonthlyRent(firstOld.getMonthlyRent());
        newRoom.setLeasePrice(firstOld.getLeasePrice());
        newRoom.setSalePrice(firstOld.getSalePrice());
        newRoom.setImage(firstOld.getImage());
        newRoom.setIntroduce(firstOld.getIntroduce());
        newRoom.setStatus(RoomStatus.VACANT.code);
        newRoom.setTenantId(currentTenantId());
        Long newRoomId = roomService.insertAndGetId(newRoom);

        // 插入合并记录
        RoomSplitMerge record = new RoomSplitMerge();
        record.setUserId(null);
        record.setUserName(null);
        record.setReasons(dto.getReasons());
        record.setType(0);
        // 同步 status (冗余写入, 兼容旧 UI 读 status 字段)
        record.setStatus(0);
        record.setOldRoomId(oldRoomIds.stream().map(String::valueOf).collect(Collectors.joining(COMMA)));
        record.setOldRoomName(oldRoomNames);
        record.setNewRoomId(String.valueOf(newRoomId));
        record.setNewRoomName(dto.getRoomName());
        record.setParkId(parkId);
        record.setTenantId(currentTenantId());
        roomSplitMergeMapper.insert(record);

        log.info("[RoomSplitMergeService] merge: oldIds={}, newId={}, reasons={}", oldRoomIds, newRoomId, dto.getReasons());
        return Result.ok(newRoom);
    }

    // ========== Split ==========

    @Transactional
    public Result<List<Room>> split(RoomSplitDTO dto) {
        Long parkId = dto.getParkId();
        Long buildingId = dto.getBuildingId();
        Long oldRoomId = dto.getOldRoomId();
        List<SplitRoomItem> roomList = dto.getRoomList();

        if (roomList == null || roomList.size() < 2) {
            throw new BizException("拆分至少需要 2 个新房间");
        }

        // 获取旧房间
        Room oldRoom = roomMapper.selectById(oldRoomId);
        if (oldRoom == null) throw new BizException("旧房间不存在");
        if (roomService.isRoomInUse(oldRoom)) {
            throw new BizException("房间 " + oldRoom.getRoomName() + " 正在使用中，不可拆分");
        }

        // 校验新房间编号唯一性(传入列表内)
        List<String> codes = roomList.stream().map(SplitRoomItem::getRoomNo).collect(Collectors.toList());
        long distinctCodes = codes.stream().distinct().count();
        if (distinctCodes != codes.size()) {
            throw new BizException("拆分房间编号不能重复");
        }
        // 校验新房间名称唯一性(传入列表内)
        List<String> names = roomList.stream().map(SplitRoomItem::getRoomName).collect(Collectors.toList());
        long distinctNames = names.stream().distinct().count();
        if (distinctNames != names.size()) {
            throw new BizException("拆分房间名称不能重复");
        }
        // 校验 DB 中是否已存在相同编号
        for (String code : codes) {
            if (existRoomNo(parkId, code, buildingId)) {
                throw new BizException("房号 " + code + " 已存在");
            }
        }
        // 校验 DB 中是否已存在相同名称
        for (String name : names) {
            if (existRoomName(parkId, name, buildingId)) {
                throw new BizException("房间名称 " + name + " 已存在");
            }
        }

        // 软删除旧房间
        roomService.batchSoftDelete(List.of(oldRoomId));

        // 创建新房间
        List<Room> newRooms = new ArrayList<>();
        for (SplitRoomItem item : roomList) {
            Room r = new Room();
            r.setParkId(parkId);
            r.setBuildingId(buildingId);
            r.setAreaId(oldRoom.getAreaId());
            r.setFloor(item.getFloor() != null ? item.getFloor() : oldRoom.getFloor());
            r.setFloorId(item.getFloorId() != null ? item.getFloorId() : oldRoom.getFloorId());
            r.setRoomNo(item.getRoomNo());
            r.setRoomName(item.getRoomName());
            r.setRoomType(item.getRoomType() != null ? item.getRoomType() : oldRoom.getRoomType());
            r.setKitId(oldRoom.getKitId());
            r.setPurposeId(oldRoom.getPurposeId());
            r.setHouseStructure(oldRoom.getHouseStructure());
            r.setAreaCovered(item.getAreaCovered());
            r.setBuildArea(item.getBuildArea());
            r.setBillableArea(item.getBillableArea());
            r.setRentingSelling(oldRoom.getRentingSelling());
            r.setLeasePrice(oldRoom.getLeasePrice());
            r.setSalePrice(oldRoom.getSalePrice());
            r.setImage(oldRoom.getImage());
            r.setIntroduce(oldRoom.getIntroduce());
            r.setStatus(RoomStatus.VACANT.code);
            r.setTenantId(currentTenantId());
            Long id = roomService.insertAndGetId(r);
            r.setId(id);
            newRooms.add(r);
        }

        // 插入拆分记录
        RoomSplitMerge record = new RoomSplitMerge();
        record.setUserId(null);
        record.setUserName(null);
        record.setReasons(dto.getReasons());
        record.setType(1);
        // 同步 status (冗余写入, 兼容旧 UI 读 status 字段)
        record.setStatus(1);
        record.setOldRoomId(String.valueOf(oldRoomId));
        record.setOldRoomName(oldRoom.getRoomName());
        record.setNewRoomId(newRooms.stream().map(r -> String.valueOf(r.getId())).collect(Collectors.joining(COMMA)));
        record.setNewRoomName(newRooms.stream().map(Room::getRoomName).collect(Collectors.joining(COMMA)));
        record.setNum(dto.getNum());
        record.setParkId(parkId);
        record.setTenantId(currentTenantId());
        roomSplitMergeMapper.insert(record);

        log.info("[RoomSplitMergeService] split: oldId={}, newIds={}, reasons={}", oldRoomId,
                record.getNewRoomId(), dto.getReasons());
        return Result.ok(newRooms);
    }

    // ========== Restore ==========

    @Transactional
    public Result<RoomSplitMerge> restore(Long roomId, int type) {
        if (type == 0) {
            return restoreMerge(roomId);
        } else {
            return restoreSplit(roomId);
        }
    }

    private Result<RoomSplitMerge> restoreMerge(Long roomId) {
        // 查找合并记录
        List<RoomSplitMerge> records = roomSplitMergeMapper.listByNewRoomId(String.valueOf(roomId));
        RoomSplitMerge record = records.stream().filter(r -> r.getType() == 0).findFirst()
                .orElseThrow(() -> new BizException("未找到合并记录"));

        // 检查合并后的房间是否在使用中
        Room mergedRoom = roomMapper.selectById(roomId);
        if (mergedRoom != null && roomService.isRoomInUse(mergedRoom)) {
            throw new BizException("合并后的房间正在使用中，不可还原");
        }

        // 软删除合并后的房间
        if (mergedRoom != null) {
            mergedRoom.setDeleted(1);
            roomMapper.updateById(mergedRoom);
        }

        // 还原旧房间 (取消软删除)
        List<String> oldIdStrs = Arrays.asList(record.getOldRoomId().split(COMMA));
        for (String idStr : oldIdStrs) {
            Room r = roomMapper.selectById(Long.valueOf(idStr.trim()));
            if (r != null) {
                r.setDeleted(0);
                r.setStatus(RoomStatus.VACANT.code);
                roomMapper.updateById(r);
            }
        }

        // 插入还原记录
        RoomSplitMerge restoreRecord = new RoomSplitMerge();
        restoreRecord.setUserId(null);
        restoreRecord.setUserName(null);
        restoreRecord.setReasons("还原合并");
        restoreRecord.setType(2);
        restoreRecord.setStatus(2);
        restoreRecord.setOldRoomId(record.getNewRoomId());
        restoreRecord.setOldRoomName(record.getNewRoomName());
        restoreRecord.setNewRoomId(record.getOldRoomId());
        restoreRecord.setNewRoomName(record.getOldRoomName());
        restoreRecord.setParkId(record.getParkId());
        restoreRecord.setTenantId(currentTenantId());
        roomSplitMergeMapper.insert(restoreRecord);

        log.info("[RoomSplitMergeService] restoreMerge: roomId={}", roomId);
        return Result.ok(restoreRecord);
    }

    private Result<RoomSplitMerge> restoreSplit(Long roomId) {
        RoomSplitMerge record = roomSplitMergeMapper.getByNewRoomId(String.valueOf(roomId));
        if (record == null || record.getType() != 1) {
            throw new BizException("未找到拆分记录");
        }

        // 检查拆分出的所有房间是否在使用中
        List<String> newIdStrs = Arrays.asList(record.getNewRoomId().split(COMMA));
        for (String idStr : newIdStrs) {
            Room r = roomMapper.selectById(Long.valueOf(idStr.trim()));
            if (r != null && roomService.isRoomInUse(r)) {
                throw new BizException("拆分后的房间 " + r.getRoomName() + " 正在使用中，不可还原");
            }
        }

        // 软删除所有拆分出的房间
        for (String idStr : newIdStrs) {
            Room r = roomMapper.selectById(Long.valueOf(idStr.trim()));
            if (r != null) {
                r.setDeleted(1);
                roomMapper.updateById(r);
            }
        }

        // 还原旧房间
        Room oldRoom = roomMapper.selectById(Long.valueOf(record.getOldRoomId()));
        if (oldRoom != null) {
            oldRoom.setDeleted(0);
            oldRoom.setStatus(RoomStatus.VACANT.code);
            roomMapper.updateById(oldRoom);
        }

        // 插入还原记录
        RoomSplitMerge restoreRecord = new RoomSplitMerge();
        restoreRecord.setUserId(null);
        restoreRecord.setUserName(null);
        restoreRecord.setReasons("还原拆分");
        restoreRecord.setType(2);
        restoreRecord.setStatus(2);
        restoreRecord.setOldRoomId(record.getNewRoomId());
        restoreRecord.setOldRoomName(record.getNewRoomName());
        restoreRecord.setNewRoomId(record.getOldRoomId());
        restoreRecord.setNewRoomName(record.getOldRoomName());
        restoreRecord.setParkId(record.getParkId());
        restoreRecord.setTenantId(currentTenantId());
        roomSplitMergeMapper.insert(restoreRecord);

        log.info("[RoomSplitMergeService] restoreSplit: roomId={}", roomId);
        return Result.ok(restoreRecord);
    }

    // ========== Helpers ==========

    private boolean existRoomNo(Long parkId, String roomNo, Long buildingId) {
        Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getParkId, parkId)
                .eq(Room::getBuildingId, buildingId)
                .eq(Room::getRoomNo, roomNo)
                .eq(Room::getDeleted, 0));
        return count != null && count > 0;
    }

    private boolean existRoomName(Long parkId, String roomName, Long buildingId) {
        Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getParkId, parkId)
                .eq(Room::getBuildingId, buildingId)
                .eq(Room::getRoomName, roomName)
                .eq(Room::getDeleted, 0));
        return count != null && count > 0;
    }

    private Long currentTenantId() {
        Long tid = TenantContextHolder.getTenantId();
        return tid != null ? tid : 1L;
    }

    @Deprecated
    public Result<Long> create(Map<String, Object> params) {
        throw new BizException("请使用 merge/split 端点");
    }
}