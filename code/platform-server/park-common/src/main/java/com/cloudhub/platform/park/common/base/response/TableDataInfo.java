package com.cloudhub.platform.park.common.base.response;

import com.cloudhub.platform.common.result.PageResult;

import java.util.List;

/**
 * 分页响应包装 (csyh cn.flyrise.common.core.page.TableDataInfo 翻译)
 * <p>csyh 出现 92 次, 是 EasyUI / LayUI 等老前端框架约定的分页字段格式:
 * {@code { total, rows, code, msg }}. 翻译策略:
 * 继承 {@link PageResult} (云枢), 字段保持兼容 (rows = records).</p>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * TableDataInfo<RoomVO> t = new TableDataInfo<>();
 * t.setRows(rooms);
 * t.setTotal(total);
 * t.setCode(200);
 * t.setMsg("查询成功");
 * return t;
 * }</pre>
 * </p>
 * <p><b>字段差异</b>:
 * <ul>
 *   <li>云枢 PageResult: {@code records} (标准命名)</li>
 *   <li>csyh TableDataInfo: {@code rows} (前端框架约定)</li>
 * </ul>
 * 业务代码可同时使用 {@code getRows()} / {@code getRecords()}, 返回同一引用.</p>
 * @see com.cloudhub.platform.common.result.PageResult 云枢分页响应 (父类)
 */
public class TableDataInfo<T> extends PageResult<T> {

    private static final long serialVersionUID = 1L;

    /** 状态码 (200=成功) — csyh 兼容字段 */
    private int code = 200;

    /** 消息 — csyh 兼容字段 */
    private String msg = "查询成功";

    public TableDataInfo() {}

    public TableDataInfo(List<T> rows, long total) {
        super(rows, total, 1, rows == null ? 0 : rows.size());
    }

    /**
     * csyh 兼容方法: 获取列表数据 (等同 getRecords)
     */
    public List<T> getRows() {
        return getRecords();
    }

    /**
     * csyh 兼容方法: 设置列表数据 (等同 setRecords)
     */
    public TableDataInfo<T> setRows(List<T> rows) {
        super.setRecords(rows);
        return this;
    }

    public int getCode() {
        return code;
    }

    public TableDataInfo<T> setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public TableDataInfo<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }
}
