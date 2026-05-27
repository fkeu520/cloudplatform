package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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
        String url = String.format("%s/user/page?userType=2&keyword=%s&status=%s&pageNum=%d&pageSize=%d",
                userServiceUrl,
                keyword != null ? keyword : "",
                status != null ? status.toString() : "",
                pageNum, pageSize);
        return restTemplate.getForObject(url, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "查询运营管理员")
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        return restTemplate.getForObject(userServiceUrl + "/user/" + id, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "新增运营管理员")
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> params) {
        params.put("userType", 2);
        return restTemplate.postForObject(userServiceUrl + "/user", params, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "更新运营管理员")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        params.put("id", id);
        restTemplate.put(userServiceUrl + "/user/" + id, params);
        return Map.of("code", 200, "message", "success");
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "删除运营管理员")
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        restTemplate.delete(userServiceUrl + "/user/" + id);
        return Map.of("code", 200, "message", "success");
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "切换状态")
    @PostMapping("/{id}/toggle-status")
    public Map<String, Object> toggleStatus(@PathVariable Long id) {
        return restTemplate.postForObject(userServiceUrl + "/user/" + id + "/toggle-status", null, Map.class);
    }

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

    @SuppressWarnings("unchecked")
    @Operation(summary = "分配用户直接授权菜单")
    @PostMapping("/{id}/menus")
    public Map<String, Object> assignMenus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return restTemplate.postForObject(userServiceUrl + "/user/" + id + "/menus", params, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "获取菜单树")
    @GetMapping("/menu/tree")
    public Map<String, Object> getMenuTree() {
        return restTemplate.getForObject(userServiceUrl + "/menu/tree", Map.class);
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "获取菜单列表")
    @GetMapping("/menu/list")
    public Map<String, Object> getMenuList() {
        return restTemplate.getForObject(userServiceUrl + "/menu/tree", Map.class);
    }
}
