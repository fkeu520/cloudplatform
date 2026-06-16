package com.cloudhub.platform.park.common.mybatis.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 园区业务实体基类 (csyh BaseEntity 翻译扩展)
 *
 * <p>云枢 {@link BaseEntity} 包含 id/createTime/updateTime/deleted 四个字段.
 * csyh BaseEntity 额外含 createBy/updateBy 字段 (操作人), 业务方在 700+ 处使用.</p>
 *
 * <p>W2.2 策略: <b>不创建 park-common 自己的 BaseEntity 包装</b> (用户决策 3, 零包装).
 * 本类仅作为"扩展基类", 业务实体继承本类即可获得 createBy/updateBy 字段.</p>
 *
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * public class Room extends ParkBaseEntity {
 *     private String name;
 *     private Long parkId;
 * }
 * }</pre>
 * </p>
 *
 * <p>字段自动填充说明:
 * <ul>
 *   <li>createBy: W3 阶段接入 LoginContextHolder (从 ThreadLocal 取 userId)</li>
 *   <li>updateBy: W3 阶段接入 LoginContextHolder (从 ThreadLocal 取 userId)</li>
 * </ul>
 * 现阶段 (W2) 需业务方在 Service.save() 前手动 setCreateBy() / setUpdateBy().</p>
 *
 * @author csyh fusion W2.2
 * @since 2026-06-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ParkBaseEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 创建人 ID (csyh 字段, W2 阶段手动设置; W3 阶段自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新人 ID (csyh 字段, W2 阶段手动设置; W3 阶段自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
