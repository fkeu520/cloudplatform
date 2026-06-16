package com.cloudhub.platform.park.common.base.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.domain.ParkUser;
import com.cloudhub.platform.park.common.base.model.query.CommonQuery;
import com.cloudhub.platform.park.common.base.model.vo.CommonVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 业务 Controller 通用接口 (csyh IBaseController 翻译)
 *
 * <p>csyh 出现 177 次, 业务模块按 CRUD 模板继承. 翻译策略:
 * 接口拆 5 个方法 (新增/修改/删除/分页/详情) + 1 个通用查询.</p>
 *
 * <p>W2 阶段: 只定义接口, 业务模块可自由选择实现方式 (实现类 / 抽象基类).
 * W3 阶段: 提供 {@code AbstractParkController} 默认实现 (含权限/分页/响应包装).</p>
 *
 * <p>使用示例:
 * <pre>{@code
 * @RestController
 * @RequestMapping("/park/room")
 * public class RoomController implements IBaseController<RoomDTO, RoomVO> {
 *     // 5 个方法 + 1 个查询
 * }
 * }</pre>
 * </p>
 *
 * @param <D> 业务 DTO (新增/修改入参)
 * @param <V> 业务 VO (返回/查询出参)
 * @author csyh fusion W2.1
 * @since 2026-06-16
 * @see com.cloudhub.platform.park.common.base.controller.AbstractParkController (W3 计划)
 */
public interface IBaseController<D, V extends CommonVO> {

    /**
     * 新增
     *
     * @param dto 入参
     * @return 200 / 500
     */
    Result<Void> create(@RequestBody D dto);

    /**
     * 修改
     *
     * @param id  主键
     * @param dto 入参
     * @return 200 / 404 / 500
     */
    Result<Void> update(@PathVariable Long id, @RequestBody D dto);

    /**
     * 删除
     *
     * @param id 主键
     * @return 200 / 404 / 500
     */
    Result<Void> delete(@PathVariable Long id);

    /**
     * 详情
     *
     * @param id 主键
     * @return VO / 404
     */
    Result<V> get(@PathVariable Long id);

    /**
     * 分页
     *
     * @param query 通用查询 (含分页 + 关键词 + parkId 范围)
     * @return 分页结果
     */
    Result<PageResult<V>> page(CommonQuery query);
}
