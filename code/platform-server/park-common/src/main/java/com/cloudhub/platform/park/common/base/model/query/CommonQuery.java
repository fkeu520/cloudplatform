package com.cloudhub.platform.park.common.base.model.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用查询条件 (csyh CommonQueryVO / ParkIdKeyWordPageQuery 翻译)
 *
 * <p>csyh 出现 140+ 次, 包含分页参数 + 关键词 + parkId 范围. 翻译策略:
 * 简化字段, 业务模块可继承扩展 (例如 RoomQuery extends CommonQuery 加 roomTypeId).</p>
 *
 * <p>W2 阶段: 包含分页 + 关键词 + 租户字段, 够覆盖 80% 业务查询.
 * W3 阶段: 增加 orgId / deptId / postId / 时间范围等扩展字段.</p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-16
 */
@Data
public class CommonQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码 (从 1 开始, 默认 1) */
    private Integer pageNum = 1;

    /** 每页大小 (默认 10) */
    private Integer pageSize = 10;

    /** 关键词 (通用模糊查询, 业务可定制) */
    private String keyword;

    /** 园区 ID (csyh pai-park-* 强依赖, 几乎所有查询都带) */
    private Long parkId;

    /** 排序字段 (默认 createTime) */
    private String orderBy = "createTime";

    /** 排序方向 (默认 desc) */
    private String orderDirection = "desc";
}
