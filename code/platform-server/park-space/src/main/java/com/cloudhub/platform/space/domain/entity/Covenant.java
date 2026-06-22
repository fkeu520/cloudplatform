package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 合同与房间关联 (park-space 业务)
 * <p>csyh 业务融合 W3.4 阶段: 合同-房间关联表 (Covenant) 简单 CRUD.</p>
 * <p>关联关系:
 * <ul>
 *   <li>room_id → sys_room.id (房间)</li>
 *   <li>covenant_id → park-contract 模块的合同 ID (跨模块, 暂不强约束)</li>
 *   <li>customer_id → 客户 ID (后续接入 park-enterprise 模块)</li>
 * </ul>
 * <p>W3 阶段仅基础 CRUD, W4+ 阶段联动合同/账单模块.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_covenant")
public class Covenant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 合同 ID (外键引用 park-contract 模块, 跨模块) */
    private Long covenantId;

    /** 合同类型: 0=租赁 1=销售 2=其他 */
    private Integer covenantType;

    /** 客户 ID */
    private Long customerId;

    /** 房间 ID (关联 sys_room.id) */
    private Long roomId;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 园区 ID */
    private Long parkId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}