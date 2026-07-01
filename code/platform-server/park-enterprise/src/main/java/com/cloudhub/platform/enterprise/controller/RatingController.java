package com.cloudhub.platform.enterprise.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.Rating;
import com.cloudhub.platform.enterprise.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业评分规则 Controller (V56)
 *
 * <p>前端 4 行配置, 一次性保存 ValidList 替换。
 *
 * @author Sisyphus (csyh 迁移)
 */
@Tag(name = "企业评分规则")
@RestController
@RequestMapping("/enterprise/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService service;

    @Operation(summary = "查询某租户全部评分规则")
    @GetMapping("/list")
    public Result<List<Rating>> list(@RequestParam(required = false) Long tenantId) {
        return Result.ok(service.listByTenant(tenantId));
    }

    @Operation(summary = "批量保存评分配置 (替换式)")
    @PutMapping("/save")
    public Result<Boolean> save(@RequestBody List<Rating> ratings) {
        return Result.ok(service.saveBatch(ratings));
    }

    @Operation(summary = "重置为默认 4 等级")
    @PostMapping("/reset")
    public Result<Boolean> reset(@RequestParam(required = false) Long tenantId) {
        return Result.ok(service.resetDefault(tenantId));
    }
}
