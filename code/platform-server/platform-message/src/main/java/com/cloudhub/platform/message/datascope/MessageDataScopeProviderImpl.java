package com.cloudhub.platform.message.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.DataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * platform-message 模块 DataScopeProvider 桩 (M5 P0-2 PR3 实施)
 *
 * <p>配套: doc/M5-P0-2-实施子任务.md §十 (PR3 启动准备)
 *
 * <h2>职责</h2>
 * <p>platform-message 启动时, DataScopeAspect 需要一个 DataScopeProvider bean.
 * <p>由于 platform-message 不依赖 platform-user, 此处提供**桩**实现, 返回 none() (无限制).
 *
 * <h2>与 UserDataScopeProviderImpl 的关系</h2>
 * <p>user 模块启动时, UserDataScopeProviderImpl 已注册, 此桩因 {@code @ConditionalOnMissingBean} 不创建.
 * <p>message 模块启动时, 没 user 依赖, 桩创建, Aspect 拿到桩返回 none().
 *
 * <h2>未来扩展</h2>
 * <p>当 platform-message 需要按消息接收人 / 部门 / 租户 过滤消息记录时,
 *    把此桩替换为真实实现, 查询 message 模块自己的 message_record 表 + user 模块的 user/role 表.
 *
 * @since 2026-06-05 (PR3 实施)
 */
@Slf4j
@Service
@ConditionalOnMissingBean(DataScopeProvider.class)
public class MessageDataScopeProviderImpl implements DataScopeProvider {

    @Override
    public DataScopeContext getContext(Long userId) {
        // platform-message 桩: 返回 none() (无 data_scope 限制)
        // 实际场景: 消息模块按 userId 直接查询, 不受 dept 树限制
        log.debug("MessageDataScopeProviderImpl.getContext: userId={}, return none()", userId);
        return DataScopeContext.none();
    }
}
