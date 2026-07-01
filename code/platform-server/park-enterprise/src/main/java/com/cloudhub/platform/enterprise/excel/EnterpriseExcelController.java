package com.cloudhub.platform.enterprise.excel;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 企业档案 Excel Controller (Phase 2.6)
 *
 * <p>导入/导出/模板下载, 业务仍走 {@code EnterpriseService.create} 以保证审计/雪花 ID.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Slf4j
@Tag(name = "企业档案 Excel 导入导出")
@RestController
@RequestMapping("/enterprise/excel")
@RequiredArgsConstructor
public class EnterpriseExcelController {

    private final EnterpriseExcelService excelService;

    @Operation(summary = "导出全部企业到 Excel")
    @GetMapping("/export")
    public void exportAll(HttpServletResponse response) throws IOException {
        excelService.exportAll(response);
    }

    @Operation(summary = "下载 Excel 模板 (空表头)")
    @GetMapping("/template")
    public void exportTemplate(HttpServletResponse response) throws IOException {
        excelService.exportTemplate(response);
    }

    @Operation(summary = "从 Excel 导入企业")
    @PostMapping("/import")
    public Result<Integer> importExcel(@RequestParam("file") MultipartFile file) {
        return excelService.importExcel(file, LoginContextHolder.getUsername());
    }
}
