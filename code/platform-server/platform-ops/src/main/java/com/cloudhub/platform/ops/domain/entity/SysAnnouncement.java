package com.cloudhub.platform.ops.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告 (W3 首页工作台公告卡片数据源)
 *
 * <p>W3 设计:
 * <ul>
 *   <li>全局公告 (app_code IS NULL): 所有用户可见, 工作台首页展示</li>
 *   <li>应用公告 (app_code='xxx'): 仅在该 app tab 下可见</li>
 *   <li>优先级 priority DESC, publish_time DESC</li>
 *   <li>expire_time: 应用层过滤 (查询时 status=0 AND (expire_time IS NULL OR expire_time &gt; NOW()))</li>
 *   <li>tenant_id: 多租户隔离 (NULL = 全租户可见)</li>
 * </ul>
 * </p>
 */
@Data
@TableName("sys_announcement")
public class SysAnnouncement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 (Markdown 或纯文本) */
    private String content;

    /**
     * 类型: 0=平台公告 1=业务公告 2=维护通知
     */
    private Integer type;

    /**
     * 限定应用 (NULL=全局), 关联 sys_app.app_code
     */
    private String appCode;

    /** 优先级 (越大越靠前) */
    private Integer priority;

    /** 状态: 0=启用 1=禁用 */
    private Integer status;

    /** 发布时间 (NULL=立即) */
    private LocalDateTime publishTime;

    /** 过期时间 (NULL=永不过期) */
    private LocalDateTime expireTime;

    /** 租户ID (NULL=全租户) */
    private Long tenantId;

    /** 创建人 */
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}