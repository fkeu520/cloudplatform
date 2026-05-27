package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_menu")
public class UserMenu {
    private Long userId;
    private Long menuId;
}
