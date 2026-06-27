package com.cloudhub.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.GrayAudit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 灰度审计 Mapper (gray-release-infrastructure PR4)
 *
 * @author cloudhub
 * @since 2026-06-27
 */
@Mapper
public interface GrayAuditMapper extends BaseMapper<GrayAudit> {
}
