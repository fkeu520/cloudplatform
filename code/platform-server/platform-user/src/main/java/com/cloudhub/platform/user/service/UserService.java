package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.UserRole;
import com.cloudhub.platform.user.domain.entity.Organization;
import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.entity.Post;
import com.cloudhub.platform.user.domain.vo.LoginVO;
import com.cloudhub.platform.user.domain.vo.UserPageVO;
import com.cloudhub.platform.user.domain.vo.UserVO;
import com.cloudhub.platform.user.domain.entity.Menu;
import com.cloudhub.platform.user.domain.entity.UserMenu;
import com.cloudhub.platform.user.domain.entity.LoginLog;
import com.cloudhub.platform.user.mapper.MenuMapper;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import com.cloudhub.platform.user.mapper.UserMenuMapper;
import com.cloudhub.platform.user.mapper.UserRoleMapper;
import com.cloudhub.platform.user.mapper.OrganizationMapper;
import com.cloudhub.platform.user.domain.mapper.DeptMapper;
import com.cloudhub.platform.user.domain.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cloudhub.platform.common.util.RsaUtil;
import org.springframework.util.DigestUtils;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMenuMapper userMenuMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final OrganizationMapper organizationMapper;
    private final DeptMapper deptMapper;
    private final PostMapper postMapper;
    private final LoginLogService loginLogService;
    private final StringRedisTemplate redisTemplate;

    private static final long TOKEN_EXPIRE_SECONDS = 7 * 24 * 3600L; // 7天
    private static final String TOKEN_PREFIX = "auth:token:";

    /**
     * 用户登录
     */
    public LoginVO login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            saveLoginLog(null, username, 0, 0L, 0, "用户名或密码错误");
            throw new BizException("用户名或密码错误");
        }
        // 解密 RSA 加密的密码
        String rawPassword;
        try {
            rawPassword = RsaUtil.decrypt(password);
        } catch (Exception e) {
            log.warn("RSA解密失败, username={}", username, e);
            throw new BizException("用户名或密码错误");
        }
        String hashedPwd = md5(rawPassword);
        if (!hashedPwd.equals(user.getPassword())) {
            saveLoginLog(user.getId(), username, user.getUserType() != null ? user.getUserType() : 0, tenantId(user), 0, "密码错误");
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            saveLoginLog(user.getId(), username, user.getUserType() != null ? user.getUserType() : 0, tenantId(user), 0, "账号已禁用");
            throw new BizException("账号已被禁用，请联系管理员");
        }

        Long tenantId = user.getTenantId() != null ? user.getTenantId().longValue() : 0L;
        Integer userType = user.getUserType();
        String token = JwtUtil.generate(user.getId().toString(), username, tenantId, userType, TOKEN_EXPIRE_SECONDS);
        long expireTime = System.currentTimeMillis() + TOKEN_EXPIRE_SECONDS * 1000;

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        saveLoginLog(user.getId(), username, user.getUserType() != null ? user.getUserType() : 0, tenantId, 1, "登录成功");

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpireTime(expireTime);
        vo.setUser(toUserVO(user));
        return vo;
    }

    private Long tenantId(User user) {
        return user.getTenantId() != null ? user.getTenantId().longValue() : 0L;
    }

    private void saveLoginLog(Long userId, String username, Integer userType, Long tenantId, Integer status, String message) {
        try {
            LoginLog log = new LoginLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setUserType(userType);
            log.setTenantId(tenantId);
            log.setLoginType(0);
            log.setStatus(status);
            log.setMessage(message);
            log.setLoginTime(LocalDateTime.now());
            loginLogService.save(log);
        } catch (Exception e) {
            log.warn("保存登录日志失败", e);
        }
    }

    /**
     * 内部验证密码（供 auth 服务调用）
     * 验证成功时同步记录登录日志
     */
    public UserVO validatePassword(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            saveLoginLog(null, username, 0, 0L, 0, "用户名或密码错误");
            throw new BizException("用户名或密码错误");
        }
        String hashedPwd = md5(password);
        if (!hashedPwd.equals(user.getPassword())) {
            saveLoginLog(user.getId(), username, user.getUserType() != null ? user.getUserType() : 0, tenantId(user), 0, "密码错误");
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            saveLoginLog(user.getId(), username, user.getUserType() != null ? user.getUserType() : 0, tenantId(user), 0, "账号已禁用");
            throw new BizException("账号已被禁用，请联系管理员");
        }
        // 登录成功，记录日志
        Long tenantId = user.getTenantId() != null ? user.getTenantId().longValue() : 0L;
        saveLoginLog(user.getId(), username, user.getUserType() != null ? user.getUserType() : 0, tenantId, 1, "登录成功");
        return toUserVO(user);
    }

    /**
     * 内部按用户名查找（供 auth 服务调用）
     */
    public UserVO getByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return toUserVO(user);
    }

    /**
     * 退出登录
     */
    public void logout(Long userId) {
        // 无需操作，Token过期即失效
    }

    /**
     * 分页查询用户
     */
    public PageResult<UserPageVO> page(String keyword, Long orgId, String orgIds, Long deptId, Long postId, Integer tenantId, Integer status, Integer userType, int pageNum, int pageSize) {
        LambdaQueryWrapper<User> wrapper = buildQueryWrapper(keyword, orgId, orgIds, deptId, postId, tenantId, status, userType);

        Page<User> p = new Page<>(pageNum, pageSize);
        Page<User> result = userMapper.selectPage(p, wrapper);

        List<UserPageVO> voList = result.getRecords().stream()
                .map(this::toUserPageVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 查询所有用户列表
     */
    public List<UserVO> list(String keyword, Long orgId, Integer status) {
        LambdaQueryWrapper<User> wrapper = buildQueryWrapper(keyword, orgId, null, null, null, null, status, null);
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(this::toUserVO).collect(Collectors.toList());
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<User> buildQueryWrapper(String keyword, Long orgId, String orgIds, Long deptId, Long postId, Integer tenantId, Integer status, Integer userType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                   .or().like(User::getNickname, keyword)
                   .or().like(User::getMobile, keyword));
        }
        if (tenantId != null) {
            wrapper.eq(User::getTenantId, tenantId);
        } else if (orgIds != null && !orgIds.isBlank()) {
            List<Long> ids = Arrays.stream(orgIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
            wrapper.in(User::getOrgId, ids);
        } else if (orgId != null) {
            wrapper.eq(User::getOrgId, orgId);
        }
        if (deptId != null) {
            wrapper.eq(User::getDeptId, deptId);
        }
        if (postId != null) {
            wrapper.eq(User::getPostId, postId);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (userType != null) {
            wrapper.eq(User::getUserType, userType);
        } else {
            // 默认排除运营管理员
            wrapper.ne(User::getUserType, 2);
        }
        wrapper.eq(User::getDeleted, 0).orderByDesc(User::getCreateTime);
        return wrapper;
    }

    /**
     * 根据ID查询用户详情
     */
    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return toUserVO(user);
    }

    /**
     * 新增用户
     */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) return Long.parseLong((String) val);
        throw new BizException("无法转换ID类型: " + val.getClass().getName());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        throw new BizException("无法转换类型: " + val.getClass().getName());
    }

    @Transactional
    public void create(Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        if (!org.springframework.util.StringUtils.hasText(username) ||
            !org.springframework.util.StringUtils.hasText(password)) {
            throw new BizException("用户名和密码不能为空");
        }

        // 检查用户名唯一
        User exist = userMapper.selectByUsername(username);
        if (exist != null) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(md5(password));
        user.setNickname((String) params.get("nickname"));
        user.setMobile((String) params.get("mobile"));
        user.setEmail((String) params.get("email"));
        user.setGender(params.get("gender") != null ? toInt(params.get("gender")) : 0);
        user.setOrgId(toLong(params.get("orgId")));
        user.setStatus(params.get("status") != null ? toInt(params.get("status")) : 1);
        user.setTenantId(params.get("tenantId") != null ? toInt(params.get("tenantId")) : 1);
        user.setUserType(params.get("userType") != null ? toInt(params.get("userType")) : 0);
        userMapper.insert(user);

        // 分配角色
        if (params.containsKey("roleIds")) {
            assignRoles(user.getId(), params.get("roleIds"));
        }
    }

    /**
     * 更新用户
     */
    @Transactional
    public void update(Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        User exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BizException("用户不存在");
        }

        if (params.containsKey("nickname")) exist.setNickname((String) params.get("nickname"));
        if (params.containsKey("mobile")) exist.setMobile((String) params.get("mobile"));
        if (params.containsKey("email")) exist.setEmail((String) params.get("email"));
        if (params.containsKey("gender")) exist.setGender(toInt(params.get("gender")));
        if (params.containsKey("orgId")) exist.setOrgId(toLong(params.get("orgId")));
        if (params.containsKey("deptId")) exist.setDeptId(toLong(params.get("deptId")));
        if (params.containsKey("postId")) exist.setPostId(toLong(params.get("postId")));
        if (params.containsKey("status")) exist.setStatus(toInt(params.get("status")));
        if (params.containsKey("avatar")) exist.setAvatar((String) params.get("avatar"));

        userMapper.updateById(exist);

        // 更新角色
        if (params.containsKey("roleIds")) {
            assignRoles(id, params.get("roleIds"));
        }
    }

    /**
     * 删除用户
     */
    @Transactional
    public void delete(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (id == 1L) {
            throw new BizException("不能删除超级管理员");
        }
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!md5(oldPassword).equals(user.getPassword())) {
            throw new BizException("原密码错误");
        }
        user.setPassword(md5(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPassword(md5(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 切换状态
     */
    public void toggleStatus(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (id == 1L) {
            throw new BizException("不能禁用超级管理员");
        }
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
    }

    /**
     * 分配角色
     */
    @Transactional
    public void assignRoles(Long userId, Object roleIdsObj) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIdsObj instanceof List) {
            for (Object obj : (List<?>) roleIdsObj) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                if (obj instanceof Number) {
                    ur.setRoleId(((Number) obj).longValue());
                } else if (obj instanceof String) {
                    ur.setRoleId(Long.parseLong((String) obj));
                }
                userRoleMapper.insert(ur);
            }
        }
    }

    /**
     * 获取用户直接授权的菜单ID列表
     */
    public List<Long> getUserMenuIds(Long userId) {
        return userMenuMapper.selectMenuIdsByUserId(userId);
    }

    /**
     * 分配用户直接授权菜单（运营管理员专用）
     */
    @Transactional
    public void assignUserMenus(Long userId, Object menuIdsObj) {
        userMenuMapper.deleteByUserId(userId);
        if (menuIdsObj instanceof List) {
            for (Object obj : (List<?>) menuIdsObj) {
                UserMenu um = new UserMenu();
                um.setUserId(userId);
                if (obj instanceof Number) {
                    um.setMenuId(((Number) obj).longValue());
                } else if (obj instanceof String) {
                    um.setMenuId(Long.parseLong((String) obj));
                }
                userMenuMapper.insert(um);
            }
        }
    }

    // ========== 内部工具方法 ==========

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (user.getGender() != null) {
            vo.setGenderDesc(user.getGender() == 1 ? "男" : user.getGender() == 2 ? "女" : "未知");
        }
        if (user.getStatus() != null) {
            vo.setStatusDesc(user.getStatus() == 1 ? "启用" : "禁用");
        }
        if (user.getUserType() != null) {
            vo.setUserType(user.getUserType());
            vo.setUserTypeDesc(user.getUserType() == 0 ? "普通用户" : user.getUserType() == 1 ? "租户管理员" : "运营管理员");
        }
        vo.setTenantId(user.getTenantId() != null ? user.getTenantId().longValue() : 0L);
        // 查询用户关联的角色ID列表
        vo.setRoleIds(userRoleMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId())
        ).stream().map(UserRole::getRoleId).collect(java.util.stream.Collectors.toList()));
        // 查询用户拥有的权限列表（角色权限 + 直接授权的菜单权限）
        List<Menu> roleMenus = menuMapper.selectByUserId(user.getId());
        List<Menu> directMenus = menuMapper.selectEnabledByUserMenuIds(user.getId());
        List<Menu> merged = new java.util.ArrayList<>(roleMenus);
        for (Menu m : directMenus) {
            if (merged.stream().noneMatch(ex -> ex.getId().equals(m.getId()))) {
                merged.add(m);
            }
        }
        vo.setPerms(merged.stream()
            .filter(m -> m.getPerms() != null && !m.getPerms().isEmpty())
            .map(Menu::getPerms)
            .collect(Collectors.toList()));
        // 查询用户角色名称列表
        List<String> roleNames = userRoleMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId())
        ).stream()
            .map(ur -> {
                Role role = roleMapper.selectById(ur.getRoleId());
                return role != null ? role.getName() : null;
            })
            .filter(r -> r != null)
            .collect(Collectors.toList());
        vo.setRoles(roleNames);
        return vo;
    }

    private UserPageVO toUserPageVO(User user) {
        UserPageVO vo = new UserPageVO();
        BeanUtils.copyProperties(user, vo);
        if (user.getStatus() != null) {
            vo.setStatusDesc(user.getStatus() == 1 ? "启用" : "禁用");
        }
        if (user.getUserType() != null) {
            vo.setUserType(user.getUserType());
            vo.setUserTypeDesc(user.getUserType() == 0 ? "普通用户" : user.getUserType() == 1 ? "租户管理员" : "运营管理员");
        }
        if (user.getOrgId() != null) {
            Organization org = organizationMapper.selectById(user.getOrgId());
            if (org != null) {
                vo.setOrgName(org.getName());
            }
        }
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        if (user.getPostId() != null) {
            Post post = postMapper.selectById(user.getPostId());
            if (post != null) {
                vo.setPostName(post.getName());
            }
        }
        return vo;
    }

    private String md5(String str) {
        return DigestUtils.md5DigestAsHex(str.getBytes(StandardCharsets.UTF_8));
    }
}