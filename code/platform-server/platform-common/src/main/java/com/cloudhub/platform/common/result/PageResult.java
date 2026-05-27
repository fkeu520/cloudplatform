package com.cloudhub.platform.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应
 *
 * @param <T> 列表项类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 当前页数据 */
    private List<T> records;

    /** 当前页码 */
    private long current;

    /** 每页大小 */
    private long size;

    public PageResult() {}

    public PageResult(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }

    /** 计算总页数 */
    public long getPages() {
        if (size == 0) return 0;
        return (total + size - 1) / size;
    }

    /** 是否有上一页 */
    public boolean hasPrevious() {
        return current > 1;
    }

    /** 是否有下一页 */
    public boolean hasNext() {
        return current < getPages();
    }
}