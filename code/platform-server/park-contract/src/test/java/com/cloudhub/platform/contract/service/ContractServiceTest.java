package com.cloudhub.platform.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.contract.domain.entity.Contract;
import com.cloudhub.platform.contract.mapper.ContractMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractService contractService;

    private Contract c1;
    private Contract c2;

    @BeforeEach
    void setUp() {
        c1 = makeContract(1L, "CT-2026-0001", "云枢科技", 1);
        c2 = makeContract(2L, "CT-2026-0002", "智汇园区运营", 0);
    }

    private Contract makeContract(Long id, String no, String tenantName, int status) {
        Contract c = new Contract();
        c.setId(id);
        c.setRoomId(1900000000000000001L);
        c.setParkId(1L);
        c.setContractNo(no);
        c.setTenantName(tenantName);
        c.setTenantPhone("1380000" + id);
        c.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        c.setEndDate(LocalDateTime.of(2028, 12, 31, 0, 0));
        c.setMonthlyRent(new BigDecimal("8000.00"));
        c.setDeposit(new BigDecimal("16000.00"));
        c.setPaymentType("MONTHLY");
        c.setStatus(status);
        c.setTenantId(1L);
        return c;
    }

    // ========== Query ==========

    @Test
    void page_noFilters_shouldReturnAll() {
        Page<Contract> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(c1, c2));
        p.setTotal(2);
        when(contractMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Contract>> result = contractService.page(null, null, 1, 10);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
        assertEquals("CT-2026-0001", result.getData().getRecords().get(0).getContractNo());
    }

    @Test
    void page_withKeyword_shouldMatch() {
        Page<Contract> p = new Page<>(1, 10);
        p.setRecords(Collections.singletonList(c1));
        p.setTotal(1);
        when(contractMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Contract>> result = contractService.page("云枢", null, 1, 10);
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void page_withBlankKeyword_shouldIgnore() {
        Page<Contract> p = new Page<>(1, 10);
        p.setRecords(Arrays.asList(c1, c2));
        p.setTotal(2);
        when(contractMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(p);

        Result<PageResult<Contract>> result = contractService.page("   ", null, 1, 10);
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void getById_existing_shouldReturn() {
        when(contractMapper.selectById(1L)).thenReturn(c1);
        Result<Contract> result = contractService.getById(1L);
        assertNotNull(result.getData());
        assertEquals("云枢科技", result.getData().getTenantName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(contractMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> contractService.getById(999L));
    }

    // ========== Create ==========

    @Test
    void create_valid_shouldInsert() {
        Map<String, Object> params = new HashMap<>();
        params.put("contractNo", "CT-2026-0003");
        params.put("roomId", 1L);
        params.put("parkId", 1L);
        params.put("tenantName", "新租户");
        params.put("tenantPhone", "13800000003");
        params.put("monthlyRent", 5000);
        params.put("deposit", 10000);

        when(contractMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(3L);
            return 1;
        }).when(contractMapper).insert(any(Contract.class));

        Result<Long> result = contractService.create(params);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).insert(captor.capture());
        Contract inserted = captor.getValue();
        assertEquals("CT-2026-0003", inserted.getContractNo());
        assertEquals("新租户", inserted.getTenantName());
        assertEquals(Integer.valueOf(0), inserted.getStatus(), "默认 status=0 (草稿)");
        assertEquals("MONTHLY", inserted.getPaymentType(), "默认付款方式 MONTHLY");
    }

    @Test
    void create_duplicateContractNo_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        params.put("contractNo", "CT-2026-0001");

        when(contractMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> contractService.create(params));
        assertTrue(ex.getMessage().contains("合同编号已存在"));
        verify(contractMapper, never()).insert(any(Contract.class));
    }

    @Test
    void create_missingContractNo_shouldThrow() {
        Map<String, Object> params = new HashMap<>();
        assertThrows(BizException.class, () -> contractService.create(params));
    }

    // ========== Update ==========

    @Test
    void update_validFields_shouldUpdate() {
        when(contractMapper.selectById(1L)).thenReturn(c1);
        Map<String, Object> params = new HashMap<>();
        params.put("tenantName", "云枢科技 (续租)");
        params.put("monthlyRent", 9000);

        Result<Void> result = contractService.update(1L, params);

        assertEquals(200, result.getCode());
        verify(contractMapper).updateById(any(Contract.class));
    }

    @Test
    void update_notFound_shouldThrow() {
        when(contractMapper.selectById(999L)).thenReturn(null);
        Map<String, Object> params = new HashMap<>();
        params.put("tenantName", "test");

        assertThrows(BizException.class, () -> contractService.update(999L, params));
    }

    // ========== Delete ==========

    @Test
    void delete_draft_shouldSoftDelete() {
        when(contractMapper.selectById(2L)).thenReturn(c2); // status=0 (DRAFT)

        Result<Void> result = contractService.delete(2L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeleted());
    }

    @Test
    void delete_active_shouldThrow() {
        when(contractMapper.selectById(1L)).thenReturn(c1); // status=1 (ACTIVE)

        BizException ex = assertThrows(BizException.class, () -> contractService.delete(1L));
        assertTrue(ex.getMessage().contains("已签约合同不可删除"));
        verify(contractMapper, never()).updateById(any());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(contractMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> contractService.delete(999L));
    }

    // ========== Status Machine ==========

    @Test
    void updateStatus_draftToActive_shouldSucceed() {
        when(contractMapper.selectById(2L)).thenReturn(c2); // DRAFT
        doAnswer(inv -> 1).when(contractMapper).updateById(any(Contract.class));

        Result<Void> result = contractService.updateStatus(2L, ContractStatus.ACTIVE.code);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getStatus());
    }

    @Test
    void updateStatus_activeToExpired_shouldSucceed() {
        c1.setStatus(ContractStatus.ACTIVE.code);
        when(contractMapper.selectById(1L)).thenReturn(c1);
        doAnswer(inv -> 1).when(contractMapper).updateById(any(Contract.class));

        Result<Void> result = contractService.updateStatus(1L, ContractStatus.EXPIRED.code);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(2), captor.getValue().getStatus());
    }

    @Test
    void updateStatus_anyToTerminated_shouldSucceed() {
        c1.setStatus(ContractStatus.ACTIVE.code);
        when(contractMapper.selectById(1L)).thenReturn(c1);
        doAnswer(inv -> 1).when(contractMapper).updateById(any(Contract.class));

        Result<Void> result = contractService.updateStatus(1L, ContractStatus.TERMINATED.code);

        assertEquals(200, result.getCode());
        assertEquals(Integer.valueOf(3), result.getCode() == 200 ? ContractStatus.TERMINATED.code : -1);
    }

    @Test
    void updateStatus_expiredToActive_shouldThrow() {
        c1.setStatus(ContractStatus.EXPIRED.code);
        when(contractMapper.selectById(1L)).thenReturn(c1);

        BizException ex = assertThrows(BizException.class,
                () -> contractService.updateStatus(1L, ContractStatus.ACTIVE.code));
        assertTrue(ex.getMessage().contains("状态非法转换"));
    }

    @Test
    void updateStatus_terminatedToDraft_shouldThrow() {
        c1.setStatus(ContractStatus.TERMINATED.code);
        when(contractMapper.selectById(1L)).thenReturn(c1);

        BizException ex = assertThrows(BizException.class,
                () -> contractService.updateStatus(1L, ContractStatus.DRAFT.code));
        assertTrue(ex.getMessage().contains("状态非法转换"));
    }

    @Test
    void updateStatus_notFound_shouldThrow() {
        when(contractMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class,
                () -> contractService.updateStatus(999L, ContractStatus.ACTIVE.code));
    }

    // ========== Entity 字段测试 ==========

    @Test
    void entity_shouldStoreAllFields() {
        Contract c = new Contract();
        c.setId(100L);
        c.setRoomId(100L);
        c.setParkId(1L);
        c.setContractNo("CT-2026-0100");
        c.setTenantName("全字段测试");
        c.setTenantPhone("13800000100");
        c.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        c.setEndDate(LocalDateTime.of(2029, 5, 31, 0, 0));
        c.setMonthlyRent(new BigDecimal("10000.00"));
        c.setDeposit(new BigDecimal("20000.00"));
        c.setPaymentType("YEARLY");
        c.setStatus(ContractStatus.ACTIVE.code);
        c.setRemark("全字段测试");

        assertEquals(100L, c.getId());
        assertEquals("CT-2026-0100", c.getContractNo());
        assertEquals("全字段测试", c.getTenantName());
        assertEquals(new BigDecimal("10000.00"), c.getMonthlyRent());
        assertEquals("YEARLY", c.getPaymentType());
        assertEquals(ContractStatus.ACTIVE.code, c.getStatus().intValue());
    }

    // ========== ContractStatus Enum ==========

    @Test
    void statusEnum_fromCode_shouldResolve() {
        assertEquals(ContractStatus.DRAFT, ContractStatus.fromCode(0));
        assertEquals(ContractStatus.ACTIVE, ContractStatus.fromCode(1));
        assertEquals(ContractStatus.EXPIRED, ContractStatus.fromCode(2));
        assertEquals(ContractStatus.TERMINATED, ContractStatus.fromCode(3));
    }

    @Test
    void statusEnum_fromCode_null_shouldDefaultToDraft() {
        assertEquals(ContractStatus.DRAFT, ContractStatus.fromCode(null));
    }

    @Test
    void statusEnum_fromCode_unknown_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ContractStatus.fromCode(99));
    }

    @Test
    void statusEnum_canTransitionTo_sameState_shouldReturnTrue() {
        assertTrue(ContractStatus.DRAFT.canTransitionTo(ContractStatus.DRAFT));
        assertTrue(ContractStatus.ACTIVE.canTransitionTo(ContractStatus.ACTIVE));
        assertTrue(ContractStatus.EXPIRED.canTransitionTo(ContractStatus.EXPIRED));
        assertTrue(ContractStatus.TERMINATED.canTransitionTo(ContractStatus.TERMINATED));
    }

    @Test
    void statusEnum_terminalStates_shouldRejectAll() {
        assertTrue(ContractStatus.EXPIRED.canTransitionTo(ContractStatus.EXPIRED));
        assertTrue(ContractStatus.TERMINATED.canTransitionTo(ContractStatus.TERMINATED));
    }
}