package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.user.domain.entity.DictData;
import com.cloudhub.platform.user.domain.entity.DictType;
import com.cloudhub.platform.user.mapper.DictDataMapper;
import com.cloudhub.platform.user.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    // ========== 字典类型管理 ==========

    public PageResult<DictType> typePage(String keyword, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<DictType> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(DictType::getDictName, keyword).or().like(DictType::getDictType, keyword);
        }
        if (status != null) w.eq(DictType::getStatus, status);
        w.eq(DictType::getDeleted, 0).orderByAsc(DictType::getId);

        Page<DictType> p = dictTypeMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public List<DictType> typeList() {
        return dictTypeMapper.selectList(
                new LambdaQueryWrapper<DictType>().eq(DictType::getDeleted, 0));
    }

    public DictType getTypeById(Long id) {
        DictType t = dictTypeMapper.selectById(id);
        if (t == null) throw new BizException("字典类型不存在");
        return t;
    }

    @Transactional
    public void createType(Map<String, Object> params) {
        String dictName = (String) params.get("dictName");
        String dictType = (String) params.get("dictType");
        if (dictName == null || dictType == null) throw new BizException("字典名称和类型编码不能为空");

        if (dictTypeMapper.selectByType(dictType) != null) {
            throw new BizException("字典类型编码已存在");
        }

        DictType t = new DictType();
        t.setDictName(dictName);
        t.setDictType(dictType);
        t.setStatus(params.get("status") != null ? ((Number) params.get("status")).intValue() : 1);
        t.setRemark((String) params.get("remark"));
        t.setTenantId(1L);
        dictTypeMapper.insert(t);
    }

    @Transactional
    public void updateType(Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        DictType t = dictTypeMapper.selectById(id);
        if (t == null) throw new BizException("字典类型不存在");

        if (params.containsKey("dictName")) t.setDictName((String) params.get("dictName"));
        if (params.containsKey("dictType")) {
            String newType = (String) params.get("dictType");
            if (!newType.equals(t.getDictType()) && dictTypeMapper.selectByType(newType) != null) {
                throw new BizException("字典类型编码已存在");
            }
            t.setDictType(newType);
        }
        if (params.containsKey("status")) t.setStatus(((Number) params.get("status")).intValue());
        if (params.containsKey("remark")) t.setRemark((String) params.get("remark"));
        dictTypeMapper.updateById(t);
    }

    @Transactional
    public void deleteType(Long id) {
        DictType t = dictTypeMapper.selectById(id);
        if (t == null) throw new BizException("字典类型不存在");
        // 级联删除字典数据
        dictDataMapper.delete(new LambdaQueryWrapper<DictData>().eq(DictData::getDictType, t.getDictType()));
        dictTypeMapper.deleteById(id);
    }

    // ========== 字典数据管理 ==========

    public PageResult<DictData> dataPage(String dictType, String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<DictData> w = new LambdaQueryWrapper<>();
        w.eq(DictData::getDictType, dictType).eq(DictData::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            w.like(DictData::getDictLabel, keyword).or().like(DictData::getDictValue, keyword);
        }
        w.orderByAsc(DictData::getDictSort);

        Page<DictData> p = dictDataMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public List<DictData> getDataByType(String dictType) {
        return dictDataMapper.selectList(
                new LambdaQueryWrapper<DictData>()
                        .eq(DictData::getDictType, dictType)
                        .eq(DictData::getStatus, 1)
                        .eq(DictData::getDeleted, 0)
                        .orderByAsc(DictData::getDictSort));
    }

    public DictData getDataById(Long id) {
        DictData d = dictDataMapper.selectById(id);
        if (d == null) throw new BizException("字典数据不存在");
        return d;
    }

    @Transactional
    public void createData(Map<String, Object> params) {
        String dictType = (String) params.get("dictType");
        String label = (String) params.get("dictLabel");
        String value = (String) params.get("dictValue");
        if (dictType == null || label == null || value == null) {
            throw new BizException("字典类型、标签和键值不能为空");
        }

        DictData d = new DictData();
        d.setDictType(dictType);
        d.setDictLabel(label);
        d.setDictValue(value);
        d.setDictSort(params.get("dictSort") != null ? ((Number) params.get("dictSort")).intValue() : 0);
        d.setStatus(params.get("status") != null ? ((Number) params.get("status")).intValue() : 1);
        d.setCssClass((String) params.get("cssClass"));
        d.setRemark((String) params.get("remark"));
        d.setTenantId(1);
        dictDataMapper.insert(d);
    }

    @Transactional
    public void updateData(Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        DictData d = dictDataMapper.selectById(id);
        if (d == null) throw new BizException("字典数据不存在");

        if (params.containsKey("dictLabel")) d.setDictLabel((String) params.get("dictLabel"));
        if (params.containsKey("dictValue")) d.setDictValue((String) params.get("dictValue"));
        if (params.containsKey("dictSort")) d.setDictSort(((Number) params.get("dictSort")).intValue());
        if (params.containsKey("status")) d.setStatus(((Number) params.get("status")).intValue());
        if (params.containsKey("cssClass")) d.setCssClass((String) params.get("cssClass"));
        if (params.containsKey("remark")) d.setRemark((String) params.get("remark"));
        dictDataMapper.updateById(d);
    }

    @Transactional
    public void deleteData(Long id) {
        if (dictDataMapper.selectById(id) == null) throw new BizException("字典数据不存在");
        dictDataMapper.deleteById(id);
    }
}
