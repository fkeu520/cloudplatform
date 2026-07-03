package com.cloudhub.platform.auth.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Step-up 二次鉴权 Token 实体
 * <p>5 分钟有效 + 单次使用 + scope 限定 + IP/UA 绑定.</p>
 * <p>配套: doc/plan/v8-P0-tenant-protection-plan.md ADR-008</p>
 * <p>存储 token_hash (SHA-256), 明文 token 永不落库.</p>
 *
 * @author cloudhub
 * @since v8.0 (2026-07)
 */
@Data
@TableName("sys_step_up_token")
public class StepUpToken {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Token SHA-256 哈希 (不存明文) */
    private String tokenHash;

    /** 签发给的 user_id */
    private Long userId;

    /** 租户ID (平台超管为 NULL) */
    private Long tenantId;

    /** 允许的操作范围 (逗号分隔, 如 "tenant:delete,refund:create") */
    private String scope;

    /** 是否单次有效 */
    private Boolean singleUse;

    /** 是否已使用 */
    private Boolean used;

    /** 签发时间 */
    private LocalDateTime issuedAt;

    /** 过期时间 (issuedAt + ttl) */
    private LocalDateTime expiresAt;

    /** 使用时间 */
    private LocalDateTime consumedAt;

    /** 签发时客户端 IP */
    private String clientIp;

    /** 签发时 UA (截断 500) */
    private String userAgent;

    /** 是否被撤销 */
    private Boolean revoked;

    /** 撤销时间 */
    private LocalDateTime revokedAt;

    /** 撤销原因 */
    private String revokeReason;
}
