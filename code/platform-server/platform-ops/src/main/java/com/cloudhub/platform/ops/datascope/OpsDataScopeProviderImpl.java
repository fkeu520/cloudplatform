package com.cloudhub.platform.ops.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.DataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * platform-ops 模块 DataScopeProvider 桩 (M5 P0-2 PR3 实施)
 * <p>配套: doc/M5-P0-2-实施子任务.md §十 (PR3 启动准备)
 * <h2>职责</h2>
 * <p>platform-ops 启动时, DataScopeAspect 需要一个 DataScopeProvider bean.
 * <p>由于 platform-ops 不依赖 platform-user (UserDataScopeProviderImpl 在 user 模块),
 *     此处提供一个**桩**实现, 返回 {@link DataScopeContext#none()} (无限制).
 * <h2>与 UserDataScopeProviderImpl 的关系</h2>
 * <p>platform-user 模块启动时, {@link com.cloudhub.platform.user.tenant.UserDataScopeProviderImpl}
 *    已注册为 DataScopeProvider. 此桩的 {@code @ConditionalOnMissingBean} 保证:
 *    <ul>
 *      <li>user 模块启动 → UserDataScopeProviderImpl 优先, 桩不创建 (避免冲突)</li>
 *      <li>ops/workflow/message 模块启动 → 没 user 依赖, 桩创建, Aspect 拿到桩返回 none()</li>
 *    </ul>
 * <h2>未来扩展</h2>
 * <p>当 platform-ops 需要独立 data_scope 逻辑 (例如租户管理按平台管理员过滤) 时,
 *    把此桩替换为真实实现, 查询 ops 模块自己的 user/role/dept 表.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(DataScopeProvider.class)
public class OpsDataScopeProviderImpl implements DataScopeProvider {

    @Override
    public DataScopeContext getContext(Long userId) {
        // platform-ops 桩: 返回 none() (无 data_scope 限制)
        // 实际场景: ops 模块多由平台超级管理员操作, data_scope=1 全部
        log.debug("OpsDataScopeProviderImpl.getContext: userId={}, return none()", userId);
        return DataScopeContext.none();
    }
}
