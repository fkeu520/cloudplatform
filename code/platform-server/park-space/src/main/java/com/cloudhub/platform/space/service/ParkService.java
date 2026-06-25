package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Park;
import com.cloudhub.platform.space.mapper.ParkMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 园区管理 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkService {

    private final ParkMapper parkMapper;
    private final ObjectMapper objectMapper;

    /** 省市区三级树（region.json 缓存） */
    private List<RegionNode> regionTree;

    @Data
    public static class RegionNode {
        private String code;
        private String name;
        private List<RegionNode> children;
    }

    /** 启动时加载省市区数据 */
    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("region.json")) {
            if (is != null) {
                regionTree = objectMapper.readValue(is, new TypeReference<List<RegionNode>>() {});
                log.info("[ParkService] 省市区数据加载完成: {} 省, {} 市, {} 区",
                        regionTree.size(),
                        regionTree.stream().mapToLong(p -> p.getChildren() != null ? p.getChildren().size() : 0).sum(),
                        regionTree.stream()
                                .flatMap(p -> p.getChildren() != null ? p.getChildren().stream() : java.util.stream.Stream.empty())
                                .mapToLong(c -> c.getChildren() != null ? c.getChildren().size() : 0).sum());
            } else {
                log.warn("[ParkService] region.json 未找到，省市区功能不可用");
                regionTree = List.of();
            }
        } catch (Exception e) {
            log.error("[ParkService] 加载省市区数据失败", e);
            regionTree = List.of();
        }
    }

    // ========== 省市区树接口 ==========

    /** 获取省市区三级树 */
    public Result<List<RegionNode>> getRegionTree() {
        return Result.ok(regionTree);
    }

    // ========== 分页查询 ==========

    public Result<PageResult<Park>> page(String keyword, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Park> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Park::getParkName, keyword)
             .or().like(Park::getAddress, keyword);
        }
        if (status != null) {
            w.eq(Park::getStatus, status);
        }
        w.eq(Park::getDeleted, 0).orderByDesc(Park::getCreateTime);

        Page<Park> p = parkMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Park> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        return Result.ok(result);
    }

    // ========== 详情 ==========

    public Result<Park> getById(Long id) {
        Park park = parkMapper.selectById(id);
        if (park == null) throw new BizException("园区不存在");
        return Result.ok(park);
    }

    // ========== 新增 ==========

    @Transactional
    public Result<Long> create(Park park) {
        // 校验名称唯一
        Long count = parkMapper.selectCount(new LambdaQueryWrapper<Park>()
                .eq(Park::getParkName, park.getParkName())
                .eq(Park::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区名称已存在: " + park.getParkName());
        }
        park.setId(null);
        park.setStatus(park.getStatus() != null ? park.getStatus() : 1);
        parkMapper.insert(park);
        log.info("[ParkService] create: id={}, name={}", park.getId(), park.getParkName());
        return Result.ok(park.getId());
    }

    // ========== 更新 ==========

    @Transactional
    public Result<Void> update(Long id, Park park) {
        Park existing = parkMapper.selectById(id);
        if (existing == null) throw new BizException("园区不存在");
        if (existing.getDeleted() == 1) throw new BizException("园区已删除");

        // 名称唯一性（排除自身）
        Long count = parkMapper.selectCount(new LambdaQueryWrapper<Park>()
                .eq(Park::getParkName, park.getParkName())
                .ne(Park::getId, id)
                .eq(Park::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区名称已存在: " + park.getParkName());
        }

        park.setId(id);
        park.setCreateTime(null);
        park.setUpdateTime(null);
        parkMapper.updateById(park);
        log.info("[ParkService] update: id={}", id);
        return Result.ok();
    }

    // ========== 删除（软删除） ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Park park = parkMapper.selectById(id);
        if (park == null) throw new BizException("园区不存在");
        park.setDeleted(1);
        parkMapper.updateById(park);
        log.info("[ParkService] delete: id={}", id);
        return Result.ok();
    }

    // ========== 启停 ==========

    @Transactional
    public Result<Void> toggleStatus(Long id, Integer status) {
        if (status != 0 && status != 1) {
            throw new BizException("状态值无效: " + status);
        }
        Park park = parkMapper.selectById(id);
        if (park == null) throw new BizException("园区不存在");
        park.setStatus(status);
        parkMapper.updateById(park);
        log.info("[ParkService] toggleStatus: id={}, status={}", id, status);
        return Result.ok();
    }

    // ========== 查询全部 ==========

    /**
     * 不分页查询所有园区（供其他模块下拉选择使用）
     * <p>包含启用与停用园区：用于筛选/编辑关联到停用园区的数据时仍需可见</p>
     */
    public Result<List<Park>> listAll() {
        List<Park> list = parkMapper.selectList(new LambdaQueryWrapper<Park>()
                .eq(Park::getDeleted, 0)
                .orderByAsc(Park::getParkName));
        return Result.ok(list);
    }
}
