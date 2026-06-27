package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.config.PlatformToggleProperties;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.GrayAudit;
import com.cloudhub.platform.user.service.GrayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 灰度开关统一查询 API (gray-release-infrastructure PR3/4)
 * <p>
 * 集中展示所有 platform.* 灰度开关当前值 + 描述 + 元数据.
 * 只读接口, 修改开关走 sys_config 流程 (M5 PR2 的 DataScopeUpgrade.vue).
 * <p>
 * 关联: doc/log/项目进度.md §灰度基础设施
 *
 * @author cloudhub
 * @since 2026-06-27
 */
@Tag(name = "灰度开关中心", description = "统一查询 platform.* 灰度开关")
@RequiredArgsConstructor
@RestController
@RequestMapping("/gray")
public class GrayController {

    private final PlatformToggleProperties toggleProperties;
    private final GrayService grayService;

    @Operation(summary = "列出所有灰度开关")
    @GetMapping("/list")
    public Result<List<GraySwitchVO>> list() {
        List<GraySwitchVO> list = new ArrayList<>();
        // ============ M4 P0-1 多租户拦截器 ============
        list.add(GraySwitchVO.builder()
                .key("platform.tenant.interceptor.enabled")
                .group("M4 P0-1 多租户拦截器")
                .value(toggleProperties.getTenant().getInterceptor().isEnabled())
                .description("TenantLineInnerInterceptor 总开关. false=禁用拦截器(紧急止血, 越权风险)")
                .restartRequired(true)
                .build());
        // ============ M5 P0-2 数据权限升级 ============
        list.add(GraySwitchVO.builder()
                .key("platform.data-scope.upgrade.enabled")
                .group("M5 P0-2 数据权限升级")
                .value(toggleProperties.getDataScope().getUpgrade().isEnabled())
                .description("v7.1 增强 (CTE + 8 个 @DataScope + 3 个 Provider 桩). false=v7.0 老 DFS")
                .restartRequired(false)
                .build());
        list.add(GraySwitchVO.builder()
                .key("platform.data-scope.upgrade.write-strict")
                .group("M5 P0-2 数据权限升级")
                .value(toggleProperties.getDataScope().getUpgrade().isWriteStrict())
                .description("PR4 写严格模式. false=WARN 放行, true=抛 DataScopeViolationException")
                .restartRequired(false)
                .build());
        // ============ gray-release-infrastructure PR5: 维度灰度 ============
        list.add(GraySwitchVO.builder()
                .key("platform.data-scope.upgrade.dimension")
                .group("M5 P0-2 数据权限升级 (维度)")
                .value(toggleProperties.getDataScope().getUpgrade().getDimension())
                .description("PR5 维度策略: all/tenant/user/percent, 控制 enabled=true 时谁走新逻辑")
                .restartRequired(false)
                .build());
        list.add(GraySwitchVO.builder()
                .key("platform.data-scope.upgrade.tenants")
                .group("M5 P0-2 数据权限升级 (维度)")
                .value(toggleProperties.getDataScope().getUpgrade().getTenants())
                .description("PR5 dimension=tenant 白名单, CSV 格式 (例 1,2,3)")
                .restartRequired(false)
                .build());
        list.add(GraySwitchVO.builder()
                .key("platform.data-scope.upgrade.users")
                .group("M5 P0-2 数据权限升级 (维度)")
                .value(toggleProperties.getDataScope().getUpgrade().getUsers())
                .description("PR5 dimension=user 白名单, CSV 格式")
                .restartRequired(false)
                .build());
        list.add(GraySwitchVO.builder()
                .key("platform.data-scope.upgrade.percent")
                .group("M5 P0-2 数据权限升级 (维度)")
                .value(toggleProperties.getDataScope().getUpgrade().getPercent())
                .description("PR5 dimension=percent 比例 0-100, 基于 userId hash 一致性")
                .restartRequired(false)
                .build());
        return Result.ok(list);
    }

    /**
     * PR4: 修改灰度开关 (upsert sys_config + 写 sys_gray_audit, 同事务)
     */
    @Operation(summary = "修改灰度开关 + 自动写审计")
    @PostMapping("/switch")
    public Result<Void> switchGray(@Valid @RequestBody GraySwitchRequest req) {
        grayService.toggle(req.getKey(), req.getValue(), req.getReason(),
                req.getOperatorId(), req.getOperatorName());
        return Result.ok();
    }

    /**
     * PR4: 审计历史分页查询
     */
    @Operation(summary = "灰度开关审计历史")
    @GetMapping("/audit/page")
    public Result<PageResult<GrayAudit>> auditPage(
            @RequestParam(name = "switchKey", required = false) String switchKey,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return Result.ok(grayService.history(switchKey, pageNum, pageSize));
    }

    /**
     * 灰度开关视图对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class GraySwitchVO {
        /** 完整配置键 (如 platform.tenant.interceptor.enabled) */
        private String key;
        /** 分组 (便于 UI 折叠显示) */
        private String group;
        /** 当前值 (从 PlatformToggleProperties 读, Nacos 热生效) */
        private Object value;
        /** 描述 (关闭影响 + 配套文档) */
        private String description;
        /** 是否需重启 (当前实现所有都 false, 未来加 Bean 重建型开关需 true) */
        private Boolean restartRequired;
        /** 查询时间 (前端刷新按钮显示) */
        private LocalDateTime queryTime;
    }

    /**
     * PR4: 修改开关请求体
     */
    @Data
    public static class GraySwitchRequest {
        @NotBlank(message = "开关 key 不能为空")
        private String key;
        @NotBlank(message = "新值不能为空")
        private String value;
        @NotBlank(message = "变更原因必填 (审计要求)")
        private String reason;
        private Long operatorId;
        private String operatorName;
    }
}