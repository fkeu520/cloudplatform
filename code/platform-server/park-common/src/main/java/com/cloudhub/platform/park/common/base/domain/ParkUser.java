package com.cloudhub.platform.park.common.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 园区用户上下文 (csyh cn.flyrise.pai.fe.common.organize.domain.User 翻译)
 *
 * <p>csyh 出现 340 次, 是跨业务最常引用的类 (任何 controller/service 都有).
 * 翻译策略: 字段全保留, 类型按云枢规范 (Long id, 雪花 ID), 不引入新业务逻辑.</p>
 *
 * <p>W2 阶段: 仅做"翻译壳", 不接入实际"当前登录用户"获取.
 * W3 阶段: 接入 platform-common LoginContextHolder, 增加 {@code current()} 静态方法.</p>
 *
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * ParkUser user = ...;  // 从 Service/Controller 传参获得
 * Long deptId = user.getDeptId();
 * String username = user.getUsername();
 * }</pre>
 * </p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-16
 * @see com.cloudhub.platform.user.entity.SysUser (云枢实体, 通过 platform-user 暴露)
 */
@Data
public class ParkUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID (雪花) */
    private Long id;

    /** 用户名 (登录账号) */
    private String username;

    /** 姓名 (显示用) */
    private String name;

    /** 租户 ID (来自 P0-1 多租户拦截器) */
    private Long tenantId;

    /** 组织/公司 ID */
    private Long orgId;

    /** 部门 ID */
    private Long deptId;

    /** 岗位 ID */
    private Long postId;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatar;

    /** 状态: 0=正常 1=停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ========== 业务角色/权限 (csyh 风格的"角色列表") ==========

    /** 角色编码列表 (如 ["admin", "manager"]) */
    private List<String> roles;

    /** 权限字符串列表 (如 ["user:add", "user:edit"]) */
    @JsonIgnore
    private Set<String> permissions;

    // ========== 兼容 csyh 字段 (UserVO 风格) ==========

    /** 部门名称 (冗余字段, 来自 dept 关联) */
    private String deptName;

    /** 组织名称 (冗余字段, 来自 org 关联) */
    private String orgName;

    /** 岗位名称 (冗余字段) */
    private String postName;
}
