package com.cloudhub.platform.user.service;

import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.entity.Organization;
import com.cloudhub.platform.user.domain.entity.Post;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.domain.entity.UserRole;
import com.cloudhub.platform.user.domain.mapper.DeptMapper;
import com.cloudhub.platform.user.domain.mapper.PostMapper;
import com.cloudhub.platform.user.domain.vo.UserVO;
import com.cloudhub.platform.user.mapper.MenuMapper;
import com.cloudhub.platform.user.mapper.OrganizationMapper;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import com.cloudhub.platform.user.mapper.UserMenuMapper;
import com.cloudhub.platform.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * UserService.getById 单元测试
 *
 * 核心测试点:
 * - toUserVO 必须正确填充 orgName/deptName/postName
 *   (前端的"编辑用户对话框"依赖这些字段回显 el-select)
 * - 如果 DB 中 orgId 找不到对应组织，orgName 应为 null
 *   (前端会触发 ensureInList 回退)
 * - 关键场景: orgId 在 DB 中存在，orgName 必须正确填充
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private UserMenuMapper userMenuMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private DeptMapper deptMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private LoginLogService loginLogService;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("getById: orgId 存在时, orgName 正确填充 (编辑回显关键路径)")
    void getById_shouldPopulateOrgNameWhenOrgExists() {
        // Given: 用户有 orgId, DB 中有对应组织
        User user = new User();
        user.setId(2061743069044281345L);
        user.setUsername("zhangs");
        user.setNickname("张三");
        user.setOrgId(2061744146233823234L);
        user.setDeptId(2061744316149272577L);
        user.setPostId(2061744676448374786L);
        user.setGender(0);
        user.setStatus(1);
        user.setUserType(0);

        when(userMapper.selectById(anyLong())).thenReturn(user);

        // Mock org 查询
        Organization org = new Organization();
        org.setId(2061744146233823234L);
        org.setName("云枢科技股份有限公司");
        when(organizationMapper.selectById(2061744146233823234L)).thenReturn(org);

        // Mock dept 查询
        Dept dept = new Dept();
        dept.setId(2061744316149272577L);
        dept.setName("人事部");
        when(deptMapper.selectById(2061744316149272577L)).thenReturn(dept);

        // Mock post 查询
        Post post = new Post();
        post.setId(2061744676448374786L);
        post.setName("人事总监");
        when(postMapper.selectById(2061744676448374786L)).thenReturn(post);

        // Mock 角色和菜单 (空)
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(menuMapper.selectByUserId(any())).thenReturn(List.of());
        when(menuMapper.selectEnabledByUserMenuIds(any())).thenReturn(List.of());

        // When
        UserVO vo = userService.getById(2061743069044281345L);

        // Then
        assertNotNull(vo);
        assertEquals("2061743069044281345", vo.getId().toString());
        assertEquals("2061744146233823234", vo.getOrgId().toString(),
                "orgId 应保持完整雪花 ID, 不被 Number 截断");
        assertEquals("云枢科技股份有限公司", vo.getOrgName(),
                "orgName 必须正确填充 - 这是编辑回显的关键");
        assertEquals("2061744316149272577", vo.getDeptId().toString());
        assertEquals("人事部", vo.getDeptName());
        assertEquals("2061744676448374786", vo.getPostId().toString());
        assertEquals("人事总监", vo.getPostName());
    }

    @Test
    @DisplayName("getById: orgId 在 DB 中不存在时, orgName 为 null (历史脏数据场景)")
    void getById_shouldReturnNullOrgNameWhenOrgNotFound() {
        // Given: 用户的 orgId 在 sys_organization 中找不到 (历史脏数据)
        User user = new User();
        user.setId(2061743069044281345L);
        user.setUsername("zhangs");
        user.setOrgId(2061744146233823200L);  // 精度丢失的脏数据
        user.setGender(0);
        user.setStatus(1);
        user.setUserType(0);

        when(userMapper.selectById(anyLong())).thenReturn(user);
        when(organizationMapper.selectById(2061744146233823200L)).thenReturn(null);  // 找不到

        // 部门和岗位未设置
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(menuMapper.selectByUserId(any())).thenReturn(List.of());
        when(menuMapper.selectEnabledByUserMenuIds(any())).thenReturn(List.of());

        // When
        UserVO vo = userService.getById(2061743069044281345L);

        // Then
        assertNotNull(vo);
        assertNull(vo.getOrgName(), "orgId 找不到对应组织时, orgName 应为 null");
        // 前端会用 ensureInList 做兜底, 把当前 orgId+orgName(null) 跳过
    }

    @Test
    @DisplayName("getById: orgId 为 null 时, orgName 应为 null")
    void getById_shouldHandleNullOrgId() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        // orgId = null
        user.setGender(0);
        user.setStatus(1);
        user.setUserType(0);

        when(userMapper.selectById(anyLong())).thenReturn(user);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(menuMapper.selectByUserId(any())).thenReturn(List.of());
        when(menuMapper.selectEnabledByUserMenuIds(any())).thenReturn(List.of());

        UserVO vo = userService.getById(1L);

        assertNotNull(vo);
        assertNull(vo.getOrgId());
        assertNull(vo.getOrgName());
    }
}
