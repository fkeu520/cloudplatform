package com.cloudhub.platform.enterprise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.enterprise.domain.entity.Enterprise;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业档案 Mapper (park-enterprise 业务)
 *
 * <p>BaseMapper 提供 CRUD: insert / update / delete (逻辑) / selectById / selectList / selectPage 等.
 * <p>Phase 1 阶段无需自定义 SQL;Phase 3 云企库对接 + 复杂报表查询可加 {@code @Select} 注解.
 */
@Mapper
public interface EnterpriseMapper extends BaseMapper<Enterprise> {
}
