package com.cloudhub.platform.enterprise.service;

import com.cloudhub.platform.enterprise.domain.entity.Rating;

import java.util.List;

/**
 * 企业评分规则 Service (V56)
 *
 * <p>每租户 4 行 (1=优 2=良 3=中 4=差), AOP RatingInitAspect 根据规则重算 percentile_score。
 *
 * @author Sisyphus (csyh 迁移)
 */
public interface RatingService {

    /**
     * 查询某租户全部评分规则
     */
    List<Rating> listByTenant(Long tenantId);

    /**
     * 批量保存 (替换式, ValidList<RatingVO>)
     */
    boolean saveBatch(List<Rating> ratings);

    /**
     * 重置为默认 4 等级规则
     */
    boolean resetDefault(Long tenantId);
}
