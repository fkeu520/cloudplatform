package com.cloudhub.platform.ops.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.ops.domain.entity.SysAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统公告 Mapper
 *
 * <p>W3 阶段新增 {@link #selectRecent}, 详见 W3.1 设计.</p>
 */
@Mapper
public interface SysAnnouncementMapper extends BaseMapper<SysAnnouncement> {

    /**
     * 查询最近公告 (工作台公告卡片数据源)
     *
     * <p>SQL 逻辑:
     * <ol>
     *   <li>status=0 (启用) + deleted=0</li>
     *   <li>publish_time 已到 (NULL OR &lt;= NOW())</li>
     *   <li>未过期 (expire_time IS NULL OR &gt; NOW())</li>
     *   <li>app_code 匹配: NULL (全局) OR 传入的 appCode</li>
     *   <li>tenant_id 匹配: NULL (全租户) OR 传入的 tenantId</li>
     *   <li>按 priority DESC, publish_time DESC 排序</li>
     *   <li>取前 limit 条</li>
     * </ol>
     * </p>
     *
     * @param appCode 应用编码 (可空: NULL 表示查全局公告, 不传表示查所有)
     * @param tenantId 租户ID (可空: NULL 表示查全租户公告)
     * @param limit 返回条数限制 (必传, 防止 SQL 不带 LIMIT)
     * @return 公告列表 (按优先级 + 发布时间降序)
     */
    List<SysAnnouncement> selectRecent(
            @Param("appCode") String appCode,
            @Param("tenantId") Long tenantId,
            @Param("limit") int limit);
}