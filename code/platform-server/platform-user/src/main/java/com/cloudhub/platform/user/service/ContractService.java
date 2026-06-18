package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Contract;
import com.cloudhub.platform.user.mapper.ContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;

    public Result<PageResult<Contract>> page(String keyword, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Contract> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank())
            w.like(Contract::getContractNo, keyword).or().like(Contract::getTenantName, keyword);
        if (status != null) w.eq(Contract::getStatus, status);
        w.eq(Contract::getDeleted, 0).orderByDesc(Contract::getId);
        Page<Contract> p = contractMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return Result.ok(new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    public Result<Contract> getById(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException("合同不存在");
        return Result.ok(c);
    }

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        String contractNo = required(params, "contractNo");
        Long count = contractMapper.selectCount(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getContractNo, contractNo).eq(Contract::getDeleted, 0));
        if (count > 0) throw new BizException("合同编号已存在: " + contractNo);

        Contract c = new Contract();
        c.setContractNo(contractNo);
        c.setRoomId(longOrNull(params, "roomId"));
        c.setParkId(longOrNull(params, "parkId"));
        c.setTenantName((String) params.get("tenantName"));
        c.setTenantPhone((String) params.get("tenantPhone"));
        c.setStartDate(dateOrNull(params.get("startDate")));
        c.setEndDate(dateOrNull(params.get("endDate")));
        c.setMonthlyRent(bdOrNull(params.get("monthlyRent")));
        c.setDeposit(bdOrNull(params.get("deposit")));
        c.setPaymentType((String) params.getOrDefault("paymentType", "MONTHLY"));
        c.setStatus(ContractStatus.DRAFT.code);
        c.setRemark((String) params.get("remark"));
        c.setTenantId(tid());

        contractMapper.insert(c);
        log.info("[ContractService] create: id={}, no={}", c.getId(), contractNo);
        return Result.ok(c.getId());
    }

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException("合同不存在");
        setIfPresent(params, "contractNo", v -> { c.setContractNo((String) v); });
        setIfPresent(params, "tenantName", v -> c.setTenantName((String) v));
        setIfPresent(params, "startDate", v -> c.setStartDate(dateOrNull(v)));
        setIfPresent(params, "endDate", v -> c.setEndDate(dateOrNull(v)));
        setIfPresent(params, "monthlyRent", v -> c.setMonthlyRent(bdOrNull(v)));
        setIfPresent(params, "deposit", v -> c.setDeposit(bdOrNull(v)));
        setIfPresent(params, "remark", v -> c.setRemark((String) v));
        contractMapper.updateById(c);
        return Result.ok();
    }

    @Transactional
    public Result<Void> updateStatus(Long id, int newStatus) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException("合同不存在");
        ContractStatus from = ContractStatus.fromCode(c.getStatus());
        ContractStatus to = ContractStatus.fromCode(newStatus);
        if (!from.canTransitionTo(to))
            throw new BizException(String.format("状态非法转换: %s→%s", from.desc, to.desc));
        c.setStatus(to.code);
        contractMapper.updateById(c);
        log.info("[ContractService] status: id={}, {}→{}", id, from.desc, to.desc);
        return Result.ok();
    }

    @Transactional
    public Result<Void> delete(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException("合同不存在");
        if (ContractStatus.ACTIVE.matches(c.getStatus()))
            throw new BizException("已签约合同不可删除");
        c.setDeleted(1);
        contractMapper.updateById(c);
        return Result.ok();
    }

    // helpers
    private String required(Map<String, Object> p, String k) {
        Object v = p.get(k);
        if (v == null || v.toString().isBlank()) throw new BizException("缺少必填字段: " + k);
        return v.toString().trim();
    }
    private Long longOrNull(Map<String, Object> p, String k) {
        Object v = p.get(k); return v == null ? null : ((Number) v).longValue();
    }
    private BigDecimal bdOrNull(Object v) {
        return v == null ? null : new BigDecimal(v.toString());
    }
    private LocalDateTime dateOrNull(Object v) {
        if (v == null) return null;
        return LocalDateTime.parse(v.toString().replace(" ", "T"));
    }
    private void setIfPresent(Map<String, Object> p, String k, java.util.function.Consumer<Object> s) {
        if (p.containsKey(k)) s.accept(p.get(k));
    }
    private Long tid() { Long t = TenantContextHolder.getTenantId(); return t != null ? t : 1L; }
}