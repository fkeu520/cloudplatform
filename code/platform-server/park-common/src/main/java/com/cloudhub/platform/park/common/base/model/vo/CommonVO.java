package com.cloudhub.platform.park.common.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务 VO 基类 (csyh 风格兼容)
 * <p>csyh 所有业务 VO 都含 id + createTime + updateTime 三个公共字段.
 * 翻译策略: 提供基类, 业务 VO 继承, 自动获得标准字段.</p>
 */
@Data
public class CommonVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 (雪花 ID) */
    private Long id;

    /** 创建时间 (ISO 8601) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 创建人 ID */
    private Long createBy;

    /** 更新人 ID */
    private Long updateBy;
}
