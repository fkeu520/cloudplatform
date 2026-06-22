package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.LandNature;
import com.cloudhub.platform.space.service.LandNatureService;
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
 * 土地性质 Controller (park-space 业务)
 * <p>W3.3 阶段: 土地性质 (LandNature) 简单 CRUD 端点.</p>
 * <ul>
 *   <li>{@code GET    /land-nature/page}      分页查询</li>
 *   <li>{@code GET    /land-nature/{id}}      详情</li>
 *   <li>{@code POST   /land-nature}           新增</li>
 *   <li>{@code PUT    /land-nature/{id}}      更新</li>
 *   <li>{@code DELETE /land-nature/{id}}      软删除</li>
 * </ul>
 */
@Tag(name = "土地性质", description = "park-space 业务 - 土地性质")
@RequiredArgsConstructor
@RestController
@RequestMapping("/land-nature")
public class LandNatureController {

    private final LandNatureService landNatureService;

    @Operation(summary = "分页查询土地性质列表")
    @GetMapping("/page")
    public Result<PageResult<LandNature>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return landNatureService.page(keyword, parkId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询土地性质详情")
    @GetMapping("/{id}")
    public Result<LandNature> getById(@PathVariable Long id) {
        return landNatureService.getById(id);
    }

    @Operation(summary = "新增土地性质")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return landNatureService.create(params);
    }

    @Operation(summary = "更新土地性质")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return landNatureService.update(id, params);
    }

    @Operation(summary = "删除土地性质 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return landNatureService.delete(id);
    }
}