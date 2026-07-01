package com.cloudhub.platform.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.enterprise.domain.entity.Rating;
import com.cloudhub.platform.enterprise.mapper.RatingMapper;
import com.cloudhub.platform.enterprise.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 企业评分规则 Service 实现 (V56)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingMapper mapper;

    @Override
    public List<Rating> listByTenant(Long tenantId) {
        return mapper.selectList(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getTenantId, tenantId)
                        .orderByAsc(Rating::getLevel));
    }

    @Override
    @Transactional
    public boolean saveBatch(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return false;
        }
        // 替换式: 先删后插
        Long tenantId = ratings.get(0).getTenantId();
        mapper.delete(new LambdaQueryWrapper<Rating>().eq(Rating::getTenantId, tenantId));
        for (Rating r : ratings) {
            mapper.insert(r);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean resetDefault(Long tenantId) {
        mapper.delete(new LambdaQueryWrapper<Rating>().eq(Rating::getTenantId, tenantId));
        // 默认 4 等级 (与 V56 seed 一致)
        long base = 1900000000000000100L;
        for (int level = 1; level <= 4; level++) {
            Rating r = new Rating();
            r.setTenantId(tenantId);
            r.setLevel(level);
            r.setOverdueMin(0);
            r.setOverdueMax(0);
            r.setDebtsMin(BigDecimal.ZERO);
            r.setDebtsMax(BigDecimal.ZERO);
            r.setDateNum(30);
            r.setDateUnit("day");
            r.setStatus(1);
            r.setId(base + level);
            mapper.insert(r);
        }
        return true;
    }
}
