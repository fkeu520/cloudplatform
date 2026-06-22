package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Floor;
import com.cloudhub.platform.space.service.FloorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 楼层 Controller (park-space 业务)
 * <p>W3.3 阶段: 楼层 (Floor) 简单 CRUD 端点.</p>
 * <ul>
 *   <li>{@code GET    /floor/page}        分页查询</li>
 *   <li>{@code GET    /floor/{id}}        详情</li>
 *   <li>{@code POST   /floor}             新增</li>
 *   <li>{@code PUT    /floor/{id}}        更新</li>
 *   <li>{@code DELETE /floor/{id}}        软删除</li>
 * </ul>
 */
@Tag(name = "楼层管理", description = "park-space 业务 - 楼层")
@RequiredArgsConstructor
@RestController
@RequestMapping("/floor")
public class FloorController {

    private final FloorService floorService;

    @Operation(summary = "分页查询楼层列表")
    @GetMapping("/page")
    public Result<PageResult<Floor>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "buildingId", required = false) Long buildingId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return floorService.page(keyword, parkId, buildingId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询楼层详情")
    @GetMapping("/{id}")
    public Result<Floor> getById(@PathVariable Long id) {
        return floorService.getById(id);
    }

    @Operation(summary = "新增楼层")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return floorService.create(params);
    }

    @Operation(summary = "更新楼层")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return floorService.update(id, params);
    }

    @Operation(summary = "删除楼层 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return floorService.delete(id);
    }
}