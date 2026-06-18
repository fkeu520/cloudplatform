package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Contract;
import com.cloudhub.platform.user.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "园区合同", description = "park-contract 业务 - 合同管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/contract")
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "分页查询合同列表")
    @GetMapping("/page")
    public Result<PageResult<Contract>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return contractService.page(keyword, status, pageNum, pageSize);
    }

    @Operation(summary = "查询合同详情")
    @GetMapping("/{id}")
    public Result<Contract> getById(@PathVariable Long id) {
        return contractService.getById(id);
    }

    @Operation(summary = "新增合同")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return contractService.create(params);
    }

    @Operation(summary = "更新合同字段 (不含状态)")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return contractService.update(id, params);
    }

    @Operation(summary = "删除合同 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return contractService.delete(id);
    }

    @Operation(summary = "状态变更 (状态机校验)")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return contractService.updateStatus(id, status);
    }
}