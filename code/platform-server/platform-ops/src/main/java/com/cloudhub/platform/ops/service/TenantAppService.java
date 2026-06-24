package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.ops.domain.entity.TenantApp;
import com.cloudhub.platform.ops.domain.mapper.TenantAppMapper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantAppService {

    private final TenantAppMapper tenantAppMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public List<Long> getAuthorizedAppIds(Long tenantId) {
        return tenantAppMapper.selectList(
                new LambdaQueryWrapper<TenantApp>()
                        .eq(TenantApp::getTenantId, tenantId)
                        .eq(TenantApp::getStatus, 1)
        ).stream().map(TenantApp::getAppId).collect(Collectors.toList());
    }

    public void authorizeApps(Long tenantId, List<Long> appIds) {
        tenantAppMapper.delete(new LambdaQueryWrapper<TenantApp>().eq(TenantApp::getTenantId, tenantId));
        if (appIds == null || appIds.isEmpty()) return;

        // O8: 使用 SqlSession BATCH 模式批量 insert，避免逐条 insert
        List<TenantApp> list = appIds.stream().map(appId -> {
            TenantApp ta = new TenantApp();
            ta.setTenantId(tenantId);
            ta.setAppId(appId);
            ta.setStatus(1);
            return ta;
        }).collect(Collectors.toList());

        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        try {
            TenantAppMapper batchMapper = sqlSession.getMapper(TenantAppMapper.class);
            for (TenantApp ta : list) {
                batchMapper.insert(ta);
            }
            sqlSession.flushStatements();
        } finally {
            sqlSession.close();
        }
    }
}
