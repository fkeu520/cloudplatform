package com.cloudhub.platform.park.common.base.response;

import com.cloudhub.platform.common.result.Result;

/**
 * 兼容 csyh 老版 AjaxResult (csyh cn.flyrise.common.core.domain.AjaxResult 翻译)
 *
 * <p>csyh 出现 97 次, 部分业务模块仍用旧名. 翻译策略:
 * 继承 {@link Result} (无泛型, 固定 {@code Result<Object>}), 静态方法完全复用.</p>
 *
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * return AjaxResult.success();           // 200 操作成功
 * return AjaxResult.success("保存成功"); // 200 + msg
 * return AjaxResult.error("操作失败");   // 500
 * return AjaxResult.error(500, "失败");  // 自定义 code
 * }</pre>
 * </p>
 *
 * <p><b>注意</b>: csyh 旧版 AjaxResult 有自己的 success/error 静态方法名 (不带 msg 参数),
 * 本类新增 {@code success(String)} / {@code error(String)} 静态方法直接转发 Result,
 * 业务代码可平滑迁移.</p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-18
 * @see com.cloudhub.platform.common.result.Result 云枢统一响应 (父类)
 */
public class AjaxResult extends Result<Object> {

    private static final long serialVersionUID = 1L;

    /**
     * 成功响应 (无消息)
     */
    public static AjaxResult success() {
        AjaxResult r = new AjaxResult();
        r.setCode(200);
        r.setMessage("操作成功");
        return r;
    }

    /**
     * 成功响应 (带消息)
     */
    public static AjaxResult success(String message) {
        AjaxResult r = new AjaxResult();
        r.setCode(200);
        r.setMessage(message);
        return r;
    }

    /**
     * 成功响应 (带数据 + 消息)
     */
    public static AjaxResult success(Object data, String message) {
        AjaxResult r = new AjaxResult();
        r.setCode(200);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    /**
     * 失败响应 (500)
     */
    public static AjaxResult error(String message) {
        AjaxResult r = new AjaxResult();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }

    /**
     * 失败响应 (自定义 code)
     */
    public static AjaxResult error(int code, String message) {
        AjaxResult r = new AjaxResult();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
