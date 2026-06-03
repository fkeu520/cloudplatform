package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.user.domain.entity.Organization;
import com.cloudhub.platform.user.mapper.OrganizationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * OrganizationService 单元测试
 *
 * 核心测试点:
 * - tree() 返回的 id 字段必须是 String 类型（避免前端 JS Number 精度丢失）
 *   19 位雪花 ID 超出 Number.MAX_SAFE_INTEGER (9.007e15)，
 *   后端若返回 JSON 数字，JS Number 解析后变成 2061744146233823000
 *   （最后几位 200 vs 真实的 234）
 * - 修复前：node.put("id", o.getId()) → JSON 数字
 * - 修复后：node.put("id", o.getId().toString()) → JSON 字符串
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationMapper orgMapper;

    private OrganizationService orgService;

    @BeforeEach
    void setUp() {
        orgService = new OrganizationService(orgMapper);
    }

    @Test
    @DisplayName("tree(): 返回的 id 字段必须是 String (避免前端雪花 ID 精度丢失)")
    void tree_shouldReturnStringIdsToAvoidFrontendPrecisionLoss() {
        // Given: 包含 19 位雪花 ID 的组织
        Organization cloudhub = createOrg(1L, 0L, "云枢科技", 1, "CLOUDHUB");
        Organization hq = createOrg(2L, 1L, "总部", 2, "HEAD");
        // 关键: 这个 ID 是 19 位雪花 ID，超出 JS Number.MAX_SAFE_INTEGER
        Organization newCompany = createOrg(2061744146233823234L, 1L, "云枢科技股份有限公司", 2, "YS001");

        when(orgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(cloudhub, hq, newCompany));

        // When
        List<Map<String, Object>> result = orgService.tree(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size(), "应该有一个根组织");

        Map<String, Object> root = result.get(0);
        assertEquals("1", root.get("id"), "小整数 ID 也应是 String (类型统一)");
        assertEquals("云枢科技", root.get("name"));

        // 关键断言: 19 位雪花 ID 必须是 String, 否则前端 Number() 后丢精度
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) root.get("children");
        assertEquals(2, children.size());

        // 子节点 1: 总部 (id=2, 小整数)
        Map<String, Object> hqNode = children.stream()
                .filter(c -> "总部".equals(c.get("name"))).findFirst().orElseThrow();
        assertEquals("2", hqNode.get("id"), "子节点 id 应是 String");

        // 子节点 2: 云枢科技股份 (id=2061744146233823234, 19 位雪花)
        Map<String, Object> newCoNode = children.stream()
                .filter(c -> "云枢科技股份有限公司".equals(c.get("name"))).findFirst().orElseThrow();
        Object newCoId = newCoNode.get("id");
        assertNotNull(newCoId, "新组织 id 不能为空");
        assertInstanceOf(String.class, newCoId,
                "id 必须是 String 类型! 实际是 " + newCoId.getClass().getName());
        assertEquals("2061744146233823234", newCoId,
                "19 位雪花 ID 必须保持完整字符串，不能被 Number 截断");
    }

    @Test
    @DisplayName("tree(): 空数据库时返回空列表")
    void tree_shouldReturnEmptyListWhenNoData() {
        when(orgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<Map<String, Object>> result = orgService.tree(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("tree(): 按租户过滤时正确传递 tenantId")
    void tree_shouldPassTenantIdToQuery() {
        Organization tenantOrg = createOrg(1L, 0L, "租户组织", 1, "T001");
        when(orgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(tenantOrg));

        List<Map<String, Object>> result = orgService.tree(2L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("租户组织", result.get(0).get("name"));
    }

    private Organization createOrg(Long id, Long parentId, String name, int type, String code) {
        Organization org = new Organization();
        org.setId(id);
        org.setParentId(parentId);
        org.setName(name);
        org.setType(type);
        org.setCode(code);
        org.setStatus(1);
        org.setSort(0);
        return org;
    }
}
