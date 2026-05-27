# -*- coding: utf-8 -*-
"""清理并重新初始化云枢中台的菜单和组织数据"""
import pymysql

conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='platform',
    password='platform123',
    database='platform',
    charset='utf8mb4',
    cursorclass=pymysql.cursors.DictCursor
)
cursor = conn.cursor()

print("=== Step 1: 清理旧数据 ===")
# 按依赖顺序删除（先清关联表）
cursor.execute("DELETE FROM sys_role_menu")
print(f"  删除了 sys_role_menu: {cursor.rowcount} 行")
cursor.execute("DELETE FROM sys_user_role")
print(f"  删除了 sys_user_role: {cursor.rowcount} 行")
cursor.execute("DELETE FROM sys_menu WHERE id > 0")
print(f"  删除了 sys_menu: {cursor.rowcount} 行")
cursor.execute("DELETE FROM sys_organization WHERE id > 0")
print(f"  删除了 sys_organization: {cursor.rowcount} 行")
conn.commit()

print("\n=== Step 2: 重新初始化组织数据 ===")
orgs = [
    (1, 0, '云枢科技', 'CLOUDHUB', 1, 0, 1, 1),
]
cursor.executemany(
    "INSERT INTO sys_organization (id, parent_id, name, code, type, sort, status, tenant_id) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)",
    orgs
)
print(f"  插入 sys_organization: {cursor.rowcount} 行")

print("\n=== Step 3: 重新初始化菜单数据 ===")
menus = [
    # 系统管理（顶级）
    (1, 0, '系统管理', '/system', 'Layout', 1, 'Setting', 100, ''),
    # 用户管理（含4个按钮权限）
    (2, 1, '用户管理', '/system/user', 'system/user/index', 1, 'User', 1, 'system:user:list'),
    (5, 2, '查看用户', None, None, 2, None, 0, 'system:user:view'),
    (6, 2, '新增用户', None, None, 2, None, 0, 'system:user:add'),
    (7, 2, '编辑用户', None, None, 2, None, 0, 'system:user:edit'),
    (8, 2, '删除用户', None, None, 2, None, 0, 'system:user:del'),
    # 角色管理
    (3, 1, '角色管理', '/system/role', 'system/role/index', 1, 'Role', 2, 'system:role:list'),
    # 菜单管理
    (4, 1, '菜单管理', '/system/menu', 'system/menu/index', 1, 'Menu', 3, 'system:menu:list'),
    # 组织管理
    (9, 1, '组织管理', '/system/org', 'system/org/index', 1, 'OfficeBuilding', 4, 'system:org:list'),
]
cursor.executemany(
    "INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort, perms) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)",
    menus
)
print(f"  插入 sys_menu: {cursor.rowcount} 行")

print("\n=== Step 4: 分配超级管理员菜单权限 ===")
cursor.execute("INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu")
print(f"  分配了 sys_role_menu: {cursor.rowcount} 行")

conn.commit()

# 验证查询
print("\n=== 验证数据 ===")
cursor.execute("SELECT id, name, code FROM sys_organization")
orgs = cursor.fetchall()
print(f"  组织数据 ({len(orgs)} 条): {[(o['id'], o['name'], o['code']) for o in orgs]}")

cursor.execute("SELECT id, parent_id, name, path, perms FROM sys_menu ORDER BY id")
menus = cursor.fetchall()
print(f"  菜单数据 ({len(menus)} 条):")
for m in menus:
    print(f"    id={m['id']}, parent={m['parent_id']}, name={m['name']}, path={m['path']}, perms={m['perms']}")

cursor.close()
conn.close()
print("\n初始化完成！")