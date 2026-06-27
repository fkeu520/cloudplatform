package com.cloudhub.platform.park.common.base.model.query;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 通用查询条件 (csyh CommonQueryVO / ParkIdKeyWordPageQuery 翻译)
 * <p>csyh 出现 140+ 次, 包含分页参数 + 关键词 + parkId 范围. 翻译策略:
 * 简化字段, 业务模块可继承扩展 (例如 RoomQuery extends CommonQuery 加 roomTypeId).</p>
 * <p>W2 阶段: 包含分页 + 关键词 + 租户字段, 够覆盖 80% 业务查询.
 * W3 阶段: 增加 orgId / deptId / postId / 时间范围等扩展字段.</p>
 */
@Data
public class CommonQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * PC2-3: orderBy 白名单 (防 SQL 注入)
     * <p>子模块可继承扩展 (如 RoomQuery.ALLOWED_ORDER_BY = {createTime, updateTime, roomNo})</p>
     */
    public static final Set<String> ALLOWED_ORDER_BY = Set.of(
            "id", "createTime", "updateTime"
    );

    /**
     * PC2-3: orderDirection 白名单
     */
    public static final Set<String> ALLOWED_ORDER_DIRECTION = Set.of("asc", "desc");

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

    /**
     * PC2-3: 安全获取 orderBy, 不在白名单时返回默认 "id"
     * <p>注: Set.of(...) 的 contains(null) 抛 NPE, 需先 null 检查.</p>
     */
    public String safeOrderBy() {
        return orderBy != null && ALLOWED_ORDER_BY.contains(orderBy) ? orderBy : "id";
    }

    /**
     * PC2-3: 安全获取 orderDirection, 不在白名单时返回 "desc"
     */
    public String safeOrderDirection() {
        return orderDirection != null && ALLOWED_ORDER_DIRECTION.contains(orderDirection)
                ? orderDirection : "desc";
    }
}
