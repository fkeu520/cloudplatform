package com.cloudhub.platform.park.common.base.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.domain.ParkUser;
import com.cloudhub.platform.park.common.base.model.query.CommonQuery;
import com.cloudhub.platform.park.common.base.model.vo.CommonVO;
import com.cloudhub.platform.park.common.base.response.R;
import com.cloudhub.platform.park.common.base.response.TableDataInfo;

import java.util.List;

/**
 * 园区业务 Controller 抽象基类 (csyh cn.flyrise.common.core.controller.BaseController 翻译)
 * <p>csyh 出现 113 次, 业务模块 controller 普遍继承. 翻译策略:
 * 提供"统一用户上下文 + 统一响应包装"基础设施, 子类专注业务.</p>
 * <p>提供能力:
 * <ul>
 *   <li>{@link #currentUser()} — 当前登录用户 (W3 接入 LoginContextHolder)</li>
 *   <li>{@link #rOk()} / {@link #rOk(Object)} — 成功响应快捷方法</li>
 *   <li>{@link #rError(String)} — 失败响应快捷方法</li>
 *   <li>{@link #table(List, long)} — 分页响应快捷方法</li>
 *   <li>{@link #pageResult(List, long)} — 云枢分页响应</li>
 * </ul>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * @RestController
 * @RequestMapping("/park/room")
 * public class RoomController extends ParkBaseController {
 *     public Result<RoomVO> getRoom(@PathVariable Long id) {
 *         RoomVO vo = roomService.getById(id);
 *         return rOk(vo);
 *     }
 * }
 * }</pre>
 * </p>
 * <p><b>W3 阶段待办</b>:
 * <ul>
 *   <li>currentUser() 接入 platform-common LoginContextHolder / JwtUtil 解析 JWT</li>
 *   <li>提供 @PreAuthorize 集成, 替代 csyh 的 Shiro 注解</li>
 * </ul>
 * @param <D> 业务 DTO
 * @param <V> 业务 VO

 * @see com.cloudhub.platform.park.common.base.controller.IBaseController 接口契约
 */
public abstract class ParkBaseController<D, V extends CommonVO> implements IBaseController<D, V> {

    // ========== 用户上下文 (W3 阶段接入 LoginContextHolder) ==========

    /**
     * 获取当前登录用户
     * <p>W2 阶段: 返回 null, 业务方需在 Controller 入参显式传 ParkUser.
     * W3 阶段: 从 JWT + Redis 解析 ParkUser 上下文.</p>
     * @return 当前 ParkUser, 未登录返回 null
     */
    protected ParkUser currentUser() {
        // TODO W3: LoginContextHolder.get() 解析 ParkUser
        return null;
    }

    /**
     * 获取当前租户 ID
     * @return 当前 tenantId, 未登录返回 null
     */
    protected Long currentTenantId() {
        ParkUser u = currentUser();
        return u == null ? null : u.getTenantId();
    }

    // ========== 统一响应快捷方法 ==========

    /**
     * 成功响应 (无数据)
     * <p>返回 {@link Result} 而非 {@link R}, 因为 R 继承 Result,
     * 调用方可用 {@code Result<Void> r = rOk();} 或 {@code R<Void> r = (R<Void>) rOk();}</p>
     */
    protected <T> Result<T> rOk() {
        return R.ok();
    }

    /**
     * 成功响应 (带数据)
     */
    protected <T> Result<T> rOk(T data) {
        return R.ok(data);
    }

    /**
     * 成功响应 (带数据 + 消息)
     */
    protected <T> Result<T> rOk(T data, String message) {
        return R.ok(data, message);
    }

    /**
     * 失败响应 (500)
     */
    protected <T> Result<T> rError(String message) {
        return R.error(message);
    }

    /**
     * 失败响应 (自定义 code)
     */
    protected <T> Result<T> rError(int code, String message) {
        return R.error(code, message);
    }

    // ========== 分页响应快捷方法 ==========

    /**
     * csyh 风格分页响应 (LayUI / EasyUI 前端)
     */
    protected <T> TableDataInfo<T> table(List<T> rows, long total) {
        return new TableDataInfo<>(rows, total);
    }

    /**
     * 云枢标准分页响应 (Element Plus 前端)
     */
    protected <T> Result<PageResult<T>> pageResult(List<T> rows, long total) {
        PageResult<T> pr = new PageResult<>(rows, total, 1, rows == null ? 0 : rows.size());
        return Result.ok(pr);
    }

    // ========== 子类必须实现的业务方法 ==========

    @Override
    public abstract Result<Void> create(D dto);

    @Override
    public abstract Result<Void> update(Long id, D dto);

    @Override
    public abstract Result<Void> delete(Long id);

    @Override
    public abstract Result<V> get(Long id);

    @Override
    public abstract Result<PageResult<V>> page(CommonQuery query);
}
