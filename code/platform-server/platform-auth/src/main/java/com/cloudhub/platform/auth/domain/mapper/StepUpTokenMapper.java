package com.cloudhub.platform.auth.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.auth.domain.StepUpToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * Step-up Token Mapper
 * <p>配套: doc/plan/v8-P0-tenant-protection-plan.md ADR-008</p>
 */
@Mapper
public interface StepUpTokenMapper extends BaseMapper<StepUpToken> {

    /**
     * 根据 token 哈希查 (verifyAndConsume 用)
     */
    StepUpToken selectByHash(@Param("tokenHash") String tokenHash);

    /**
     * 撤销某用户所有未使用的 step-up token (改密码 / 主动踢下时调用)
     * @return 受影响行数
     */
    int revokeAllUnused(@Param("userId") Long userId,
                        @Param("now") LocalDateTime now,
                        @Param("reason") String reason);
}
