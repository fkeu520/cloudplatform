package com.cloudhub.platform.ops.service;

import com.cloudhub.platform.ops.domain.entity.SysAnnouncement;
import com.cloudhub.platform.ops.domain.mapper.SysAnnouncementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 系统公告 Service
 *
 * <p>W3 阶段: 仅 /announcement/recent 端点. W4 阶段加完整 CRUD.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAnnouncementService {

    private final SysAnnouncementMapper announcementMapper;

    /**
     * 查询最近公告 (工作台公告卡片数据源)
     *
     * @param appCode  应用编码 (可空: NULL 表示查全局公告)
     * @param tenantId 租户 ID (可空: NULL 表示全租户)
     * @param limit    返回条数限制 (建议 1-10)
     * @return 公告列表 (空列表表示无公告, 不抛异常)
     */
    public List<SysAnnouncement> recent(String appCode, Long tenantId, int limit) {
        if (limit <= 0 || limit > 50) {
            log.warn("[Announcement] limit 越界, 自动修正为 3: limit={}", limit);
            limit = 3;
        }
        try {
            return announcementMapper.selectRecent(appCode, tenantId, limit);
        } catch (Exception e) {
            log.error("[Announcement] recent 查询异常: appCode={}, tenantId={}", appCode, tenantId, e);
            return Collections.emptyList();
        }
    }
}