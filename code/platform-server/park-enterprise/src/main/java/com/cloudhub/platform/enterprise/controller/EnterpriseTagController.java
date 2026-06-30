package com.cloudhub.platform.enterprise.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseTag;
import com.cloudhub.platform.enterprise.service.EnterpriseTagService;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业标签 Controller (park-enterprise Phase 1)
 */
@Tag(name = "企业标签", description = "park-enterprise 业务 - 企业标签管理")
@RestController
@RequestMapping("/enterprise/tag")
@RequiredArgsConstructor
public class EnterpriseTagController {

    private final EnterpriseTagService tagService;

    /** 列出指定企业的标签 */
    @GetMapping("/list")
    public Result<List<EnterpriseTag>> listByEnterprise(@RequestParam Long enterpriseId) {
        return tagService.listByEnterprise(enterpriseId);
    }

    /** 列出所有标签 (字典下拉用) */
    @GetMapping("/all")
    public Result<List<EnterpriseTag>> listAll() {
        return tagService.listAll();
    }

    /** 新增 */
    @PostMapping
    public Result<EnterpriseTag> create(@RequestBody EnterpriseTag tag) {
        return tagService.create(tag, LoginContextHolder.getUsername());
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return tagService.delete(id);
    }
}
