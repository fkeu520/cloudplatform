rtk : [rtk] /!\ No hook installed ??run `rtk init -g` for automatic token savings
???? ?:1 ??: 400
+ ... LS='false'; rtk git show feat/m5-p0-2-pr4-write-strict:code/platform- ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: ([rtk] /!\ No ho...c token savings:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.domain.vo.LoginVO;
import com.cloudhub.platform.user.domain.vo.UserPageVO;
import com.cloudhub.platform.user.domain.vo.UserVO;
import com.cloudhub.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "??????", description = "???CRUD/???/??????/??????")
@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    // ========== ?????? ==========

    @Operation(summary = "??????")
    @Log(title = "??????", businessType = 0)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || password == null) {
            throw new BizException("???????????????");
        }
        LoginVO vo = userService.login(username, password);
        return Result.ok(vo);
    }

    @Operation(summary = "???????????????")
    @GetMapping("/info")
    public Result<UserVO> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new BizException("?????);
        }
        return Result.ok(userService.getById(Long.parseLong(userId)));
    }

    @Operation(summary = "???????)
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            userService.logout(Long.parseLong(userId));
        }
        return Result.ok();
    }

    @Operation(summary = "???????????? auth ????????)
    @PostMapping("/internal/validate")
    public Result<UserVO> validatePassword(@RequestBody @Validated Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || password == null) {
            throw new BizException("???????????????");
        }
        UserVO vo = userService.validatePassword(username, password);
        return Result.ok(vo);
    }

    @Operation(summary = "?????????????????? auth ????????)
    @GetMapping("/internal/by-username/{username}")
    public Result<UserVO> getByUsername(@PathVariable String username) {
        return Result.ok(userService.getByUsername(username));
    }

    // ========== ?????? ==========

    @Operation(summary = "?????????")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "orgId", required = false) Long orgId,
            @RequestParam(name = "orgIds", required = false) String orgIds,
            @RequestParam(name = "deptId", required = false) Long deptId,
            @RequestParam(name = "postId", required = false) Long postId,
            @RequestParam(name = "tenantId", required = false) Integer tenantId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "userType", required = false) Integer userType,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return Result.ok(userService.page(keyword, orgId, orgIds, deptId, postId, tenantId, status, userType, pageNum, pageSize));
    }

    @Operation(summary = "?????????????)
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "orgId", required = false) Long orgId,
            @RequestParam(name = "status", required = false) Integer status
    ) {
        return Result.ok(userService.list(keyword, orgId, status));
    }

    @Operation(summary = "???ID??????")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(userService.getById(id));
    }

    @Operation(summary = "??????")
    @Log(title = "??????", businessType = 1)
    @PostMapping
    public Result<Void> create(@RequestBody @Validated Map<String, Object> params) {
        userService.create(params);
        return Result.ok();
    }

    @Operation(summary = "??????")
    @Log(title = "??????", businessType = 2)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable(name = "id") Long id, @RequestBody @Validated Map<String, Object> params) {
        params.put("id", id);
        userService.update(params);
        return Result.ok();
    }

    @Operation(summary = "??????")
    @Log(title = "??????", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "??????")
    @PostMapping("/{id}/password")
    public Result<Void> changePassword(
            @PathVariable(name = "id") Long id,
            @RequestBody @Validated Map<String, String> params
    ) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            throw new BizException("?????????????????);
        }
        userService.changePassword(id, oldPassword, newPassword);
        return Result.ok();
    }

    @Operation(summary = "??????")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(
            @PathVariable(name = "id") Long id,
            @RequestBody @Validated Map<String, String> params
    ) {
        String newPassword = params.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new BizException("???????????);
        }
        userService.resetPassword(id, newPassword);
        return Result.ok();
    }

    @Operation(summary = "???????????/?????)
    @PostMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable(name = "id") Long id) {
        userService.toggleStatus(id);
        return Result.ok();
    }

    @Operation(summary = "??????")
    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable(name = "id") Long id, @RequestBody Map<String, Object> params) {
        userService.assignRoles(id, params.get("roleIds"));
        return Result.ok();
    }

    @Operation(summary = "?????????????????D???")
    @GetMapping("/{id}/menuIds")
    public Result<List<Long>> getUserMenuIds(@PathVariable(name = "id") Long id) {
        return Result.ok(userService.getUserMenuIds(id));
    }

    @Operation(summary = "?????????????????????????????)
    @PostMapping("/{id}/menus")
    public Result<Void> assignUserMenus(@PathVariable(name = "id") Long id, @RequestBody Map<String, Object> params) {
        userService.assignUserMenus(id, params.get("menuIds"));
        return Result.ok();
    }
}
