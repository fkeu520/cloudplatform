package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseTag;
import com.cloudhub.platform.enterprise.mapper.EnterpriseTagMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EnterpriseTagService} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseTagServiceTest {

    @Mock
    private EnterpriseTagMapper tagMapper;

    @InjectMocks
    private EnterpriseTagService tagService;

    private EnterpriseTag tag(Long id, Long enterpriseId, String name) {
        EnterpriseTag t = new EnterpriseTag();
        t.setId(id);
        t.setEnterpriseId(enterpriseId);
        t.setTagName(name);
        t.setTagColor("#1890ff");
        t.setSortOrder(0);
        return t;
    }

    @Test
    void listByEnterprise_shouldFilterById() {
        when(tagMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(tag(1L, 10L, "VIP"), tag(2L, 10L, "高新")));

        Result<List<EnterpriseTag>> r = tagService.listByEnterprise(10L);

        assertEquals(200, r.getCode());
        assertEquals(2, r.getData().size());
    }

    @Test
    void create_valid_shouldSetAutoFields() {
        when(tagMapper.insert(any(EnterpriseTag.class))).thenReturn(1);
        EnterpriseTag input = tag(null, 100L, "战略客户");

        Result<EnterpriseTag> r = tagService.create(input, "admin");

        assertEquals(200, r.getCode());
        ArgumentCaptor<EnterpriseTag> cap = ArgumentCaptor.forClass(EnterpriseTag.class);
        verify(tagMapper).insert(cap.capture());
        assertNull(cap.getValue().getId());
        assertEquals("admin", cap.getValue().getCreateBy());
    }

    @Test
    void create_missingFields_shouldThrow() {
        EnterpriseTag input = new EnterpriseTag(); // 无 enterpriseId 与 tagName
        assertThrows(IllegalArgumentException.class, () -> tagService.create(input, "admin"));
    }

    @Test
    void delete_shouldInvokeLogicalDelete() {
        when(tagMapper.deleteById(1L)).thenReturn(1);

        Result<Void> r = tagService.delete(1L);

        assertEquals(200, r.getCode());
        verify(tagMapper).deleteById(1L);
    }
}
