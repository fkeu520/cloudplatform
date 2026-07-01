package com.cloudhub.platform.enterprise.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业档案 Excel 导入导出 DTO (Phase 2.6)
 *
 * <p>对应 sys_enterprise 表, 字段与 Enterprise 实体对应, 减少手工映射.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@NoArgsConstructor
public class EnterpriseExcelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "企业名称", index = 0)
    private String name;

    @ExcelProperty(value = "简称", index = 1)
    private String alias;

    @ExcelProperty(value = "英文名", index = 2)
    private String engName;

    @ExcelProperty(value = "纳税人识别号", index = 3)
    private String taxNumber;

    @ExcelProperty(value = "统一社会信用代码", index = 4)
    private String creditCode;

    @ExcelProperty(value = "行业", index = 5)
    private String industry;

    @ExcelProperty(value = "法人姓名", index = 6)
    private String legalPersonName;

    @ExcelProperty(value = "企业类型", index = 7)
    private String companyOrgType;

    @ExcelProperty(value = "注册资本", index = 8)
    private String regCapital;

    @ExcelProperty(value = "实收资本", index = 9)
    private String actualCapital;

    @ExcelProperty(value = "成立日期", index = 10)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estiblishTime;

    @ExcelProperty(value = "省份", index = 11)
    private String base;

    @ExcelProperty(value = "市", index = 12)
    private String city;

    @ExcelProperty(value = "区", index = 13)
    private String district;

    @ExcelProperty(value = "注册地址", index = 14)
    private String regLocation;

    @ExcelProperty(value = "登记机关", index = 15)
    private String regInstitute;

    @ExcelProperty(value = "企业状态", index = 16)
    private String regStatus;

    @ExcelProperty(value = "人员规模", index = 17)
    private String staffNumRange;

    @ExcelProperty(value = "经营范围", index = 18)
    private String businessScope;

    @ExcelProperty(value = "联系电话", index = 19)
    private String phoneNumber;

    @ExcelProperty(value = "邮箱", index = 20)
    private String email;

    @ExcelProperty(value = "状态(1启用0停用)", index = 21)
    private Integer status;
}
