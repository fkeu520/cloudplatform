package com.cloudhub.platform.enterprise.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseEntBind;
import com.cloudhub.platform.enterprise.service.EnterpriseEntBindService;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业绑定关系 Controller (park-enterprise Phase 1)
 *
 * <p>绑定类型 (park/building/tenant/room) 由前端传入,service 不强制 enum 化(便于 Phase 2 扩展).
 */
@Tag(name = "企业绑定", description = "park-enterprise 业务 - 企业↔园区/楼栋/租户 绑定")
@RestController
@RequestMapping("/enterprise/bind")
@RequiredArgsConstructor
public class EnterpriseEntBindController {

    private final EnterpriseEntBindService bindService;

    /** 列出企业的绑定 */
    @GetMapping("/list")
    public Result<List<EnterpriseEntBind>> listByEnterprise(@RequestParam Long enterpriseId) {
        return bindService.listByEnterprise(enterpriseId);
    }

    /** 全部绑定 (admin) */
    @GetMapping("/all")
    public Result<List<EnterpriseEntBind>> listAll() {
        return bindService.listAll();
    }

    /** 绑定 */
    @PostMapping
    public Result<EnterpriseEntBind> bind(@RequestBody EnterpriseEntBind bind) {
        return bindService.bind(bind, LoginContextHolder.getUsername());
    }

    /** 解绑 (置 bind_status=0) */
    @PatchMapping("/{id}/unbind")
    public Result<Void> unbind(@PathVariable Long id) {
        return bindService.unbind(id);
    }

    /** 真删 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return bindService.delete(id);
    }
}
