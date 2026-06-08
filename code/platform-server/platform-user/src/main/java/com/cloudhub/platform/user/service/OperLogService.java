package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudhub.platform.common.annotation.DataScope;
import com.cloudhub.platform.user.domain.entity.OperLog;
import com.cloudhub.platform.user.mapper.OperLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OperLogService extends ServiceImpl<OperLogMapper, OperLog> {

    public void insertOperLog(OperLog operLog) {
        save(operLog);
    }

    /**
     * M5 P0-2 PR4: 操作日志分页 (加 @DataScope)
     * <p>
     * 业务场景: 审计查询 "运维部 5 月份所有操作" / "销售 1 组这个月谁动了客户数据"
     * <p>
     * - scope=1 (全部) → 不加条件 (管理员视角, 看全公司)
     * - scope=2 (本部门) → AND dept_id = 当前用户部门
     * - scope=3 (本部门及下级) → AND dept_id IN (CTE 收集的子部门)
     * - scope=4 (本人) → AND dept_id = 当前用户部门 (oper_log 无 operator_id 字段, 用 dept_id 等价)
     * - scope=5 (自定义) → AND dept_id IN (customDeptIds)
     * <p>
     * 注意:
     * 1. 必须加在 Impl 类方法上 (CGLIB 代理可识别), 不能加接口方法
     * 2. ServiceImpl.page() 内部用 baseMapper.selectPage(), MyBatis-Plus 拦截器链
     *    (DataScopeInnerInterceptor) 会自动改写 SQL WHERE
     * 3. 该方法替代 OperLogController 直接调用 service.page(page, query) 的旧路径
     */
    @DataScope(deptAlias = "dept_id")
    public IPage<OperLog> pageList(Page<OperLog> page, Wrapper<OperLog> queryWrapper) {
        return baseMapper.selectPage(page, queryWrapper);
    }
}

