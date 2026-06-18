package com.cloudhub.platform.park.common.base.constants;

/**
 * 字段名常量 (csyh cn.flyrise.pai.fe.common.constants.FieldConstants 翻译)
 * <p>csyh 出现 113 次, 业务模块通过 {@code FieldConstants.ID} 引用字段名, 避免硬编码.
 * 翻译策略: 保留常量名 + 值 (与 csyh 完全一致), 方便批量替换.</p>
 * <p>W2 阶段: 仅枚举 csyh 强引用的 ID/Name/Status 等. 业务模块如有更多,
 * 在各自模块的 {@code constants} 包下扩展.</p>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * wrapper.eq(FieldConstants.ID, dto.getId());
 * }</pre>
 * </p>
 */
public final class FieldConstants {

    private FieldConstants() {}

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String CODE = "code";
    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String SORT = "sort";
    public static final String REMARK = "remark";
    public static final String PARENT_ID = "parent_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String DEPT_ID = "dept_id";
    public static final String ORG_ID = "org_id";
    public static final String POST_ID = "post_id";
    public static final String USER_ID = "user_id";
    public static final String PARK_ID = "park_id";
    public static final String CREATE_TIME = "create_time";
    public static final String UPDATE_TIME = "update_time";
    public static final String CREATE_BY = "create_by";
    public static final String UPDATE_BY = "update_by";
    public static final String DELETED = "deleted";
}
