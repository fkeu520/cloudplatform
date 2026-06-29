package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "运营管理员管理", description = "运营平台用户CRUD/菜单授权")
@RequiredArgsConstructor
@RestController
@RequestMapping("/ops-user")
public class OpsUserController {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://platform-user:8081}")
    private String userServiceUrl;

    @SuppressWarnings("unchecked")
    @Operation(summary = "分页查询运营管理员")
    @GetMapping("/page")
    public Map<String, Object> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        String url = UriComponentsBuilder.fromHttpUrl(userServiceUrl + "/user/page")
                .queryParam("userType", "2")
                .queryParam("keyword", keyword != null ? keyword : "")
                .queryParam("status", status != null ? status.toString() : "")
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .build()
                .toUriString();
        return restTemplate.getForObject(url, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "查询运营管理员")
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        return restTemplate.getForObject(userServiceUrl + "/user/" + id, Map.class);
    }

    @Log(title = "运营管理员", businessType = 1)
    @SuppressWarnings("unchecked")
    @Operation(summary = "新增运营管理员")
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> params) {
        params.put("userType", 2);
        return restTemplate.postForObject(userServiceUrl + "/user", params, Map.class);
    }

    @Log(title = "运营管理员", businessType = 2)
    @SuppressWarnings("unchecked")
    @Operation(summary = "更新运营管理员")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        params.put("id", id);
        restTemplate.put(userServiceUrl + "/user/" + id, params);
        return Map.of("code", 200, "message", "success");
    }

    @Log(title = "运营管理员", businessType = 3)
    @SuppressWarnings("unchecked")
    @Operation(summary = "删除运营管理员")
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        restTemplate.delete(userServiceUrl + "/user/" + id);
        return Map.of("code", 200, "message", "success");
    }

    @Log(title = "运营管理员", businessType = 2)
    @SuppressWarnings("unchecked")
    @Operation(summary = "切换状态")
    @PostMapping("/{id}/toggle-status")
    public Map<String, Object> toggleStatus(@PathVariable Long id) {
        return restTemplate.postForObject(userServiceUrl + "/user/" + id + "/toggle-status", null, Map.class);
    }

    @Log(title = "运营管理员", businessType = 2)
    @SuppressWarnings("unchecked")
    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        return restTemplate.postForObject(userServiceUrl + "/user/" + id + "/reset-password", params, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "获取用户直接授权的菜单ID列表")
    @GetMapping("/{id}/menuIds")
    public Map<String, Object> getMenuIds(@PathVariable Long id) {
        return restTemplate.getForObject(userServiceUrl + "/user/" + id + "/menuIds", Map.class);
    }

    @Log(title = "运营管理员", businessType = 2)
    @SuppressWarnings("unchecked")
    @Operation(summary = "分配用户直接授权菜单")
    @PostMapping("/{id}/menus")
    public Map<String, Object> assignMenus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return restTemplate.postForObject(userServiceUrl + "/user/" + id + "/menus", params, Map.class);
    }

    /**
     * 获取菜单树 (ops-admin 平台)
     *
     * <p>OPA2-4 后继修复 (#36.2): 不再代理 platform-user /menu/tree。
     * 原实现会把 admin-platform 全量 sys_menu (角色/部门/... N 个) 一并返回,
     * 导致 ops-admin (8090) 左侧菜单出现 admin-platform 的菜单项, 用户串扰。</p>
     *
     * <p>当前处理: 返回空, ops-admin Layout.vue 收到空 data 后走前端 FALLBACK_MENUS
     * (8 个硬编码 ops 菜单: 租户/存储/网关/审计/ops-user/monitor/message/record/ops-entry)。
     * 短期方案: 隔离 OK; 长期方案入后续 sprint — 新增 sys_app `ops-admin` + sys_menu 行
     * 后改回 {@code /menu/user?appId=<ops-admin-id>}。</p>
     */
    @SuppressWarnings("unchecked")
    @Operation(summary = "获取菜单树 (ops-admin, 临时返回空, 详见 #36.2)")
    @GetMapping("/menu/tree")
    public Map<String, Object> getMenuTree() {
        return java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "获取菜单列表 (ops-admin, 临时返回空, 详见 #36.2)")
    @GetMapping("/menu/list")
    public Map<String, Object> getMenuList() {
        return java.util.Collections.emptyMap();
    }
}
