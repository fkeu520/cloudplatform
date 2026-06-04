package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.user.domain.entity.Config;
import com.cloudhub.platform.user.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigMapper configMapper;

    public PageResult<Config> page(String keyword, Integer configType, int pageNum, int pageSize) {
        LambdaQueryWrapper<Config> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Config::getConfigName, keyword).or().like(Config::getConfigKey, keyword);
        }
        if (configType != null) w.eq(Config::getConfigType, configType);
        w.eq(Config::getDeleted, 0).orderByAsc(Config::getId);

        Page<Config> p = configMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public Config getById(Long id) {
        Config c = configMapper.selectById(id);
        if (c == null) throw new BizException("参数不存在");
        return c;
    }

    public Config getByKey(String configKey) {
        Config c = configMapper.selectByKey(configKey);
        if (c == null) throw new BizException("参数不存在");
        return c;
    }

    @Transactional
    public void create(Map<String, Object> params) {
        String configName = (String) params.get("configName");
        String configKey = (String) params.get("configKey");
        String configValue = (String) params.get("configValue");
        if (configName == null || configKey == null || configValue == null) {
            throw new BizException("参数名称、键名和键值不能为空");
        }

        if (configMapper.selectByKey(configKey) != null) {
            throw new BizException("参数键名已存在");
        }

        Config c = new Config();
        c.setConfigName(configName);
        c.setConfigKey(configKey);
        c.setConfigValue(configValue);
        c.setConfigType(params.get("configType") != null ? ((Number) params.get("configType")).intValue() : 0);
        c.setRemark((String) params.get("remark"));
        c.setTenantId(1L);
        configMapper.insert(c);
    }

    @Transactional
    public void update(Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        Config c = configMapper.selectById(id);
        if (c == null) throw new BizException("参数不存在");

        if (params.containsKey("configName")) c.setConfigName((String) params.get("configName"));
        if (params.containsKey("configKey")) c.setConfigKey((String) params.get("configKey"));
        if (params.containsKey("configValue")) c.setConfigValue((String) params.get("configValue"));
        if (params.containsKey("configType")) c.setConfigType(((Number) params.get("configType")).intValue());
        if (params.containsKey("remark")) c.setRemark((String) params.get("remark"));
        configMapper.updateById(c);
    }

    @Transactional
    public void delete(Long id) {
        Config c = configMapper.selectById(id);
        if (c == null) throw new BizException("参数不存在");
        configMapper.deleteById(id);
    }
}
