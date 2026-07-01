package com.cloudhub.platform.enterprise.excel;

import com.alibaba.excel.EasyExcel;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.Enterprise;
import com.cloudhub.platform.enterprise.mapper.EnterpriseMapper;
import com.cloudhub.platform.enterprise.service.EnterpriseService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 企业档案 Excel 导入导出 Service (Phase 2.6)
 *
 * <p>使用 EasyExcel 3.3.4 读写 xlsx, 默认 sheet 名称 "企业档案".
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseExcelService {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseService enterpriseService;

    /** 导出全部企业到 xlsx */
    public void exportAll(HttpServletResponse response) throws IOException {
        List<Enterprise> list = enterpriseMapper.selectList(null);
        List<EnterpriseExcelDTO> rows = mapToDto(list);
        writeToResponse(response, rows);
    }

    /** 模板下载 */
    public void exportTemplate(HttpServletResponse response) throws IOException {
        writeToResponse(response, new ArrayList<>());
    }

    /** 导入 xlsx */
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> importExcel(MultipartFile file, String createBy) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件为空");
        }
        try {
            List<EnterpriseExcelDTO> rows = EasyExcel.read(file.getInputStream())
                    .head(EnterpriseExcelDTO.class)
                    .sheet()
                    .doReadSync();
            int count = 0;
            for (EnterpriseExcelDTO dto : rows) {
                if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
                    continue; // 跳过空行/无名
                }
                enterpriseService.create(toEntity(dto), createBy);
                count++;
            }
            log.info("[enterprise-excel] 导入成功 count={}", count);
            return Result.ok(count);
        } catch (IOException e) {
            throw new BizException("解析 Excel 失败: " + e.getMessage());
        }
    }

    /** 实体 → DTO */
    private List<EnterpriseExcelDTO> mapToDto(List<Enterprise> entities) {
        List<EnterpriseExcelDTO> result = new ArrayList<>(entities.size());
        for (Enterprise e : entities) {
            EnterpriseExcelDTO dto = new EnterpriseExcelDTO();
            dto.setName(e.getName());
            dto.setAlias(e.getAlias());
            dto.setEngName(e.getEngName());
            dto.setTaxNumber(e.getTaxNumber());
            dto.setCreditCode(e.getCreditCode());
            dto.setIndustry(e.getIndustry());
            dto.setLegalPersonName(e.getLegalPersonName());
            dto.setCompanyOrgType(e.getCompanyOrgType());
            dto.setRegCapital(e.getRegCapital());
            dto.setActualCapital(e.getActualCapital());
            dto.setEstiblishTime(e.getEstiblishTime());
            dto.setBase(e.getBase());
            dto.setCity(e.getCity());
            dto.setDistrict(e.getDistrict());
            dto.setRegLocation(e.getRegLocation());
            dto.setRegInstitute(e.getRegInstitute());
            dto.setRegStatus(e.getRegStatus());
            dto.setStaffNumRange(e.getStaffNumRange());
            dto.setBusinessScope(e.getBusinessScope());
            dto.setPhoneNumber(e.getPhoneNumber());
            dto.setEmail(e.getEmail());
            dto.setStatus(e.getStatus());
            result.add(dto);
        }
        return result;
    }

    /** DTO → 实体 */
    private Enterprise toEntity(EnterpriseExcelDTO dto) {
        Enterprise e = new Enterprise();
        e.setName(dto.getName());
        e.setAlias(dto.getAlias());
        e.setEngName(dto.getEngName());
        e.setTaxNumber(dto.getTaxNumber());
        e.setCreditCode(dto.getCreditCode());
        e.setIndustry(dto.getIndustry());
        e.setLegalPersonName(dto.getLegalPersonName());
        e.setCompanyOrgType(dto.getCompanyOrgType());
        e.setRegCapital(dto.getRegCapital());
        e.setActualCapital(dto.getActualCapital());
        e.setEstiblishTime(dto.getEstiblishTime());
        e.setBase(dto.getBase());
        e.setCity(dto.getCity());
        e.setDistrict(dto.getDistrict());
        e.setRegLocation(dto.getRegLocation());
        e.setRegInstitute(dto.getRegInstitute());
        e.setRegStatus(dto.getRegStatus());
        e.setStaffNumRange(dto.getStaffNumRange());
        e.setBusinessScope(dto.getBusinessScope());
        e.setPhoneNumber(dto.getPhoneNumber());
        e.setEmail(dto.getEmail());
        e.setStatus(Objects.equals(dto.getStatus(), null) ? 1 : dto.getStatus());
        return e;
    }

    /** 写入响应流 */
    private void writeToResponse(HttpServletResponse response, List<EnterpriseExcelDTO> rows) throws IOException {
        String fileName = URLEncoder.encode("企业档案_" + LocalDateTime.now()
                .toString().replace(":", "-").substring(0, 19) + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        try (OutputStream os = response.getOutputStream()) {
            EasyExcel.write(os, EnterpriseExcelDTO.class)
                    .sheet("企业档案")
                    .doWrite(rows);
        }
    }
}
