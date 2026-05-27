package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 角色菜单关联实体
 */
@Data
@TableName("sys_role_menu")
public class RoleMenu {

    private Long roleId;

    private Long menuId;
}