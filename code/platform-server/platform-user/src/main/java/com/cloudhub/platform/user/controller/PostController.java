package com.cloudhub.platform.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.user.domain.entity.Post;
import com.cloudhub.platform.user.domain.mapper.PostMapper;
import com.cloudhub.platform.user.domain.service.PostService;
import com.cloudhub.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "岗位管理", description = "岗位CRUD")
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final PostMapper postMapper;
    
    @Operation(summary = "根据ID查询岗位")
    @GetMapping("/{id}")
    public Result<Post> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(postService.getById(id));
    }
    
    @Operation(summary = "根据组织ID查询岗位列表")
    @GetMapping("/org/{orgId}")
    public Result<List<Post>> listByOrgId(@PathVariable(name = "orgId") Long orgId) {
        return Result.ok(postService.listByOrgId(orgId));
    }
    
    @Operation(summary = "根据部门ID查询岗位列表")
    @GetMapping("/dept/{deptId}")
    public Result<List<Post>> listByDeptId(@PathVariable(name = "deptId") Long deptId) {
        return Result.ok(postService.listByDeptId(deptId));
    }
    
    @Operation(summary = "分页查询岗位")
    @GetMapping("/page")
    public Result<IPage<Post>> pageList(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "orgId", required = false) Long orgId,
            @RequestParam(name = "deptId", required = false) Long deptId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1).eq(Post::getDeleted, 0);
        if (orgId != null) {
            wrapper.eq(Post::getOrgId, orgId);
        }
        if (deptId != null) {
            wrapper.eq(Post::getDeptId, deptId);
        }
        wrapper.orderByAsc(Post::getSort);
        IPage<Post> page = postMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(page);
    }
    
    @Operation(summary = "新增岗位")
    @PostMapping
    public Result<Post> create(@RequestBody Post post) {
        return Result.ok(postService.create(post));
    }
    
    @Operation(summary = "更新岗位")
    @PutMapping("/{id}")
    public Result<Post> update(@PathVariable(name = "id") Long id, @RequestBody Post post) {
        post.setId(id);
        return Result.ok(postService.update(post));
    }
    
    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        postService.delete(id);
        return Result.ok();
    }
}