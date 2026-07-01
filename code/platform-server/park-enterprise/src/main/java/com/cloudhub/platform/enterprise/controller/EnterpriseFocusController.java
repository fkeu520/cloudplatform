package com.cloudhub.platform.enterprise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseFocus;
import com.cloudhub.platform.enterprise.service.EnterpriseFocusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 企业关注标签关联 Controller (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Tag(name = "企业关注标签")
@RestController
@RequestMapping("/enterprise/ent-focus")
@RequiredArgsConstructor
public class EnterpriseFocusController {

    private final EnterpriseFocusService service;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<IPage<EnterpriseFocus>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Long focusId) {
        IPage<EnterpriseFocus> page = service.pageList(new Page<>(current, size), enterpriseId, focusId);
        return Result.ok(page);
    }

    @Operation(summary = "新增 (企业选择关注标签+内容)")
    @PostMapping
    public Result<EnterpriseFocus> save(@RequestBody EnterpriseFocus entity) {
        return Result.ok(service.save(entity));
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody EnterpriseFocus entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @Operation(summary = "软删")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(service.deleteById(id));
    }
}
