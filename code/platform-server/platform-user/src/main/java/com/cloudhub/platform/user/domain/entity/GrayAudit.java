package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 灰度开关审计实体 (gray-release-infrastructure PR4)
 * <p>
 * 记录所有 platform.* 灰度开关的变更, 含操作人/原因/服务/IP.
 * <p>
 * 关联: doc/log/项目进度.md §灰度基础设施
 *
 * @author cloudhub
 * @since 2026-06-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_gray_audit")
public class GrayAudit extends BaseEntity {
    /** 配置键 (如 platform.data-scope.upgrade.enabled) */
    private String switchKey;
    /** 分组 (M4 P0-1 / M5 P0-2 等, 冗余便于查询) */
    private String groupName;
    /** 旧值 */
    private String oldValue;
    /** 新值 */
    private String newValue;
    /** 操作类型: NACOS_PUSH / DB_UPDATE / ENV_RESTART / BEAN_REFRESH / INIT */
    private String opType;
    /** 操作人 ID (sys_user.id, DB 方式时记录; Nacos 推送时为 NULL) */
    private Long operatorId;
    /** 操作人姓名 (冗余便于查询) */
    private String operatorName;
    /** 变更原因 (业务方填写) */
    private String reason;
    /** 变更发生服务 (如 platform-user) */
    private String serviceName;
    /** 变更实例 IP */
    private String instanceIp;
    /** 创建时间 (覆盖 BaseEntity 的 createTime, 显式声明便于 MyBatis-Plus 映射) */
    private LocalDateTime createTime;
}