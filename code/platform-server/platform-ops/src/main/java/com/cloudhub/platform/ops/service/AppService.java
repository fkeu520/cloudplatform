package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.ops.domain.entity.App;
import com.cloudhub.platform.ops.domain.mapper.AppMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppService {

    private final AppMapper appMapper;

    public IPage<App> page(String keyword, Integer appType, int pageNum, int pageSize) {
        LambdaQueryWrapper<App> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(App::getAppName, keyword).or().like(App::getAppCode, keyword);
        }
        if (appType != null) wrapper.eq(App::getAppType, appType);
        wrapper.orderByAsc(App::getSort);
        return appMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<App> list(Integer appType) {
        LambdaQueryWrapper<App> wrapper = new LambdaQueryWrapper<>();
        if (appType != null) wrapper.eq(App::getAppType, appType);
        wrapper.orderByAsc(App::getSort);
        return appMapper.selectList(wrapper);
    }

    public App getById(Long id) {
        return appMapper.selectById(id);
    }

    public void create(Map<String, Object> params) {
        App app = new App();
        app.setAppName((String) params.get("appName"));
        app.setAppCode((String) params.get("appCode"));
        app.setAppIcon((String) params.get("appIcon"));
        app.setAppType(params.get("appType") != null ? Integer.parseInt(params.get("appType").toString()) : 0);
        app.setStatus(1);
        app.setSort(params.get("sort") != null ? Integer.parseInt(params.get("sort").toString()) : 0);
        app.setRemark((String) params.get("remark"));
        appMapper.insert(app);
    }

    public void update(Long id, Map<String, Object> params) {
        App app = appMapper.selectById(id);
        if (app == null) throw new BizException("应用不存在");
        if (params.containsKey("appName")) app.setAppName((String) params.get("appName"));
        if (params.containsKey("appCode")) app.setAppCode((String) params.get("appCode"));
        if (params.containsKey("appIcon")) app.setAppIcon((String) params.get("appIcon"));
        if (params.containsKey("remark")) app.setRemark((String) params.get("remark"));
        if (params.containsKey("sort")) app.setSort(Integer.parseInt(params.get("sort").toString()));
        appMapper.updateById(app);
    }

    public void delete(Long id) {
        appMapper.deleteById(id);
    }
}
