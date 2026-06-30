package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseEntBind;
import com.cloudhub.platform.enterprise.mapper.EnterpriseEntBindMapper;
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
 * {@link EnterpriseEntBindService} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseEntBindServiceTest {

    @Mock
    private EnterpriseEntBindMapper bindMapper;

    @InjectMocks
    private EnterpriseEntBindService bindService;

    private EnterpriseEntBind make(Long id, Long entId, String type, Long bindId, int status) {
        EnterpriseEntBind b = new EnterpriseEntBind();
        b.setId(id);
        b.setEnterpriseId(entId);
        b.setBindType(type);
        b.setBindId(bindId);
        b.setBindStatus(status);
        return b;
    }

    @Test
    void listByEnterprise_returnsActiveBindings() {
        when(bindMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(
                        make(1L, 10L, "park", 1L, 1),
                        make(2L, 10L, "building", 5L, 1)));

        Result<List<EnterpriseEntBind>> r = bindService.listByEnterprise(10L);

        assertEquals(200, r.getCode());
        assertEquals(2, r.getData().size());
    }

    @Test
    void bind_valid_shouldSetActiveStatusAndTimestamps() {
        when(bindMapper.insert(any(EnterpriseEntBind.class))).thenReturn(1);
        EnterpriseEntBind input = make(null, 100L, "room", 202L, 0);

        Result<EnterpriseEntBind> r = bindService.bind(input, "admin");

        assertEquals(200, r.getCode());
        ArgumentCaptor<EnterpriseEntBind> cap = ArgumentCaptor.forClass(EnterpriseEntBind.class);
        verify(bindMapper).insert(cap.capture());
        EnterpriseEntBind captured = cap.getValue();
        assertEquals(1, captured.getBindStatus());   // 强制 1
        assertNotNull(captured.getBindingAt());
        assertEquals("admin", captured.getCreateBy());
    }

    @Test
    void bind_missingFields_shouldThrow() {
        EnterpriseEntBind input = new EnterpriseEntBind(); // 全空
        assertThrows(IllegalArgumentException.class, () -> bindService.bind(input, "admin"));
    }

    @Test
    void unbind_shouldSetStatus0AndTimestamp() {
        when(bindMapper.selectById(1L)).thenReturn(make(1L, 10L, "park", 1L, 1));

        Result<Void> r = bindService.unbind(1L);

        assertEquals(200, r.getCode());
        ArgumentCaptor<EnterpriseEntBind> cap = ArgumentCaptor.forClass(EnterpriseEntBind.class);
        verify(bindMapper).updateById(cap.capture());
        assertEquals(0, cap.getValue().getBindStatus());
        assertNotNull(cap.getValue().getUnboundAt());
    }

    @Test
    void unbind_notFound_shouldThrow() {
        when(bindMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> bindService.unbind(99L));
    }
}
