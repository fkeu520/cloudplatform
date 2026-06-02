package com.cloudhub.platform.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.mapper.DeptMapper;
import com.cloudhub.platform.user.domain.service.DeptService;
import com.cloudhub.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理", description = "部门CRUD/树形结构")
@RestController
@RequestMapping("/dept")
@RequiredArgsConstructor
public class DeptController {
    
    private final DeptService deptService;
    private final DeptMapper deptMapper;
    
    @Operation(summary = "根据ID查询部门")
    @GetMapping("/{id}")
    public Result<Dept> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(deptService.getById(id));
    }
    
    @Operation(summary = "根据组织ID查询部门列表")
    @GetMapping("/org/{orgId}")
    public Result<List<Dept>> listByOrgId(@PathVariable(name = "orgId") Long orgId) {
        return Result.ok(deptService.listByOrgId(orgId));
    }
    
    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    public Result<List<Dept>> listTree(@RequestParam(name = "orgId", required = false) Long orgId) {
        List<Dept> depts;
        if (orgId != null) {
            depts = deptService.listTreeByOrgId(orgId);
        } else {
            LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Dept::getStatus, 1).eq(Dept::getDeleted, 0);
            depts = deptMapper.selectList(wrapper);
            depts = deptService.listTreeByOrgId(0L);
        }
        return Result.ok(depts);
    }
    
    @Operation(summary = "分页查询部门")
    @GetMapping("/page")
    public Result<IPage<Dept>> pageList(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "orgId", required = false) Long orgId) {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dept::getStatus, 1).eq(Dept::getDeleted, 0);
        if (orgId != null) {
            wrapper.eq(Dept::getOrgId, orgId);
        }
        wrapper.orderByAsc(Dept::getSort);
        IPage<Dept> page = deptMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(page);
    }
    
    @Operation(summary = "新增部门")
    @PostMapping
    public Result<Dept> create(@RequestBody Dept dept) {
        return Result.ok(deptService.create(dept));
    }
    
    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    public Result<Dept> update(@PathVariable(name = "id") Long id, @RequestBody Dept dept) {
        dept.setId(id);
        return Result.ok(deptService.update(dept));
    }
    
    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}