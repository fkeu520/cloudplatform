package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户信息 (park-enterprise V58)
 *
 * <p>对应 csyh {@code CustomerInformationPO} (V2022031401)。
 * 客户拓展调查表, 50+ 字段分 6 大类: 基本/业务/IP/财务/需求/物理。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_customer_information")
public class CustomerInformation extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID (P0-1 拦截器) */
    private Long tenantId;

    /** 园区 ID (多园区区分) */
    private Long parkId;

    // ===== 基本信息 =====

    /** 关联企业 ID (sys_enterprise.id, 可空=未关联) */
    private Long enterpriseId;

    /** 项目编号 */
    private String code;

    /** 客户类型 1=潜在 2=意向 3=已签约 */
    private Integer customerType;

    /** 租用面积 (m²) */
    private BigDecimal area;

    /** 入驻地址 */
    private String settleAddress;

    /** 生成链接 (客户专属填报链接) */
    private String url;

    /** 负责人姓名 */
    private String name;

    /** 电话 */
    private String phone;

    /** 客户手机号 (短信验证) */
    private String managerPhone;

    /** 邮箱 */
    private String email;

    /** 注册时间 */
    private LocalDateTime registTime;

    /** 注册资本 (元) */
    private BigDecimal registMoney;

    /** 租金标准 (元/m²/月) */
    private String rentalStandard;

    // ===== 产业领域 =====

    /** 所属产业领域 1=集成电路 2=生物医药 3=新材料 4=新能源 5=智能制造 6=信创 */
    private Integer industrialField;

    /** 其他产业领域 */
    private String industrialFieldOther;

    /** 主营业务 */
    private String mainBusiness;

    // ===== 企业实力 =====

    /** 企业实力 1=上市公司 2=规上 3=独角兽 4=瞪羚 5=专精特新 6=高企培育 7=科技型中小 8=外资 9=市高级人才 10=其他 */
    private String companyStrengths;

    /** 其他企业实力 */
    private String companyStrengthsOther;

    // ===== 知识产权 (8 项) =====

    /** 有效知识产权总数量 */
    private Integer validIntellectualProperty;

    /** 发明专利数量 */
    private Integer inventionPatents;

    /** 实用新型专利数量 */
    private Integer utilityModelPatent;

    /** 外观设计专利数量 */
    private Integer industrialDesignPatents;

    /** 商标数量 */
    private Integer trademark;

    /** 软件著作权 */
    private Integer softwareCopyright;

    /** 植物新品种 */
    private Integer newPlantVariety;

    /** 集成电路布图 */
    private Integer integratedCircuitLayout;

    /** 购买国外专利 */
    private Integer purchaseForeignPatents;

    /** 其他专利 */
    private Integer otherPatents;

    // ===== 财务状况 =====

    /** 营业收入 1=500万以下 2=500-2000万 3=2000万-1亿 4=1亿以上 */
    private Integer businessIncome;

    /** 上年度税收 1=10万以下 2=10-50万 3=50-100万 4=100-300万 5=300-500万 6=500万以上 */
    private Integer lastYearTax;

    /** 投资总金额 (元) */
    private BigDecimal totalInvestmentAmount;

    /** 融资总金额 (元) */
    private BigDecimal totalFinancingAmount;

    // ===== 困难 + 需求 =====

    /** 面临困难 1=市场需求不足 2=资金紧张 3=融资渠道狭窄 4=人才短缺 5=其他 */
    private String companyDifficulties;

    /** 其他困难 */
    private String companyDifficultiesOther;

    /** 配套服务需求 1=班车 2=儿童托管 3=网络通讯 4=团餐 5=公寓 6=室内环境 */
    private String supportingServices;

    /** 科技咨询服务 1=项目申报 2=高企认定 3=成果鉴定 4=知识产权贯标 5=创新平台认定 6=政策推送解读 */
    private String technicalConsultingServices;

    /** 管理服务 1=工商注册 2=法律服务 3=人力资源 4=团建 5=培训 6=其他 */
    private String managementServices;

    /** 其他管理服务 */
    private String managementServicesOther;

    /** 公共技术平台 1=基础研发 2=中试 3=分析检测 4=成果转化 5=技术合作 6=仪器共享 7=其他 */
    private String technologyPlatformServices;

    /** 其他公共技术平台 */
    private String technologyPlatformServicesOther;

    /** 投资服务 1=基金 2=VC/PE 3=金融贷款 4=融资租赁 5=租金换股权 6=产权换股权 7=其他 */
    private String investmentServices;

    /** 其他投资服务 */
    private String investmentServicesOther;

    /** 对产业园建议 */
    private String suggest;

    // ===== 物理需求 =====

    /** 结构荷载 1=2kN/m²以下 2=2-3kN/m² 3=3-5kN/m² 4=5kN/m²以上 */
    private Integer structuralLoad;

    /** 楼层高度 1=3.8m以下 2=3.8-4.5m 3=4.5m以上 */
    private Integer floorHeight;

    /** 电容量 1=100KW及以下 2=500KW及以下 3=1000KW及以下 */
    private Integer capacitance;

    /** 1=需要给水 2=需要排水 */
    private String supplyAndDrainage;

    /** 新风排烟 1=需要 2=不需要 */
    private Integer freshAirSmokeExhaust;

    /** 电梯长度 (mm) */
    private Integer elevatorLength;

    /** 电梯宽度 (mm) */
    private Integer elevatorWidth;

    /** 电梯高度 (mm) */
    private Integer elevatorHeight;

    /** 电梯荷载 1=1000kg 2=1350kg 3=1600kg 4=其他 */
    private Integer elevatorLoad;

    // ===== 自身状态 =====

    /** 启用状态 (1=有效 0=失效) */
    private Integer status;
}
