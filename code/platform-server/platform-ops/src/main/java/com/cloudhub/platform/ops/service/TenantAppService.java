package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.ops.domain.entity.TenantApp;
import com.cloudhub.platform.ops.domain.mapper.TenantAppMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantAppService {

    private final TenantAppMapper tenantAppMapper;

    public List<Long> getAuthorizedAppIds(Long tenantId) {
        return tenantAppMapper.selectList(
                new LambdaQueryWrapper<TenantApp>()
                        .eq(TenantApp::getTenantId, tenantId)
                        .eq(TenantApp::getStatus, 1)
        ).stream().map(TenantApp::getAppId).collect(Collectors.toList());
    }

    public void authorizeApps(Long tenantId, List<Long> appIds) {
        tenantAppMapper.delete(new LambdaQueryWrapper<TenantApp>().eq(TenantApp::getTenantId, tenantId));
        for (Long appId : appIds) {
            TenantApp ta = new TenantApp();
            ta.setTenantId(tenantId);
            ta.setAppId(appId);
            ta.setStatus(1);
            tenantAppMapper.insert(ta);
        }
    }
}
