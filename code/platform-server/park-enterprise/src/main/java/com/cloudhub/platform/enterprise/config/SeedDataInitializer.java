package com.cloudhub.platform.enterprise.config;

import com.cloudhub.platform.enterprise.cloud.mapper.CloudDataMapper;
import com.cloudhub.platform.enterprise.cloud.mapper.EnterpriseOverviewMapper;
import com.cloudhub.platform.enterprise.cloud.mapper.EntRegTypeMapper;
import com.cloudhub.platform.enterprise.cloud.mapper.NationalEconomyMapper;
import com.cloudhub.platform.enterprise.cloud.model.CloudData;
import com.cloudhub.platform.enterprise.cloud.model.EntRegType;
import com.cloudhub.platform.enterprise.cloud.model.EnterpriseOverview;
import com.cloudhub.platform.enterprise.cloud.model.NationalEconomy;
import com.cloudhub.platform.enterprise.domain.entity.*;
import com.cloudhub.platform.enterprise.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 真实企业模拟数据初始化 (Phase 3)
 *
 * <p>原计划用 V64 Flyway migration, 但 V64 因列名不匹配/重复主键反复失败.
 * 改为应用层 {@link CommandLineRunner} — 在 Spring 容器启动后、所有 bean 就绪后执行,
 * 绕开 Flyway 失败时 {@code SqlSessionFactory} 缺失的启动阻塞.
 *
 * <p>幂等设计: 启动时检查 {@code sys_enterprise.id=1} 是否存在, 存在则跳过.
 *
 * <p>5 家中国头部企业: 华为/腾讯/比亚迪/阿里/宁德时代 (tenant_id=1).
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SeedDataInitializer implements CommandLineRunner {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseAddressMapper enterpriseAddressMapper;
    private final EnterpriseIntroductionMapper enterpriseIntroductionMapper;
    private final EnterpriseRegisterMapper enterpriseRegisterMapper;
    private final EnterpriseTagMapper enterpriseTagMapper;
    private final EnterpriseIndustryMapper enterpriseIndustryMapper;
    private final FocusMapper focusMapper;
    private final FocusItemMapper focusItemMapper;
    private final EnterpriseFocusMapper enterpriseFocusMapper;
    private final CustomerInformationMapper customerInformationMapper;
    private final CloudDataMapper cloudDataMapper;
    private final EnterpriseOverviewMapper enterpriseOverviewMapper;
    private final EntRegTypeMapper enterpriseRegTypeMapper;
    private final NationalEconomyMapper nationalEconomyMapper;

    private static final String CB = "system";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        if (enterpriseMapper.selectById(1L) != null) {
            log.info("[SeedDataInitializer] seed 已存在, 跳过 (id=1)");
            return;
        }
        log.info("[SeedDataInitializer] 开始 seed 5 家真实企业...");

        seedFocus();
        seedNationalEconomyAndRegType();
        seedEnterprises();
        seedAddresses();
        seedIntroductions();
        seedRegisters();
        seedTags();
        seedIndustries();
        seedEnterpriseFocus();
        seedCustomerInfo();
        seedCloudData();
        seedOverview();

        log.info("[SeedDataInitializer] seed 完成: 5 企业 + 70+ 关联数据");
    }

    private void seedFocus() {
        Focus f1 = new Focus(); f1.setId(1L); f1.setTenantId(1L); f1.setName("企业规模"); f1.setSorting(1); f1.setStatus(1); focusMapper.insert(f1);
        Focus f2 = new Focus(); f2.setId(2L); f2.setTenantId(1L); f2.setName("企业类型"); f2.setSorting(2); f2.setStatus(1); focusMapper.insert(f2);
        Focus f3 = new Focus(); f3.setId(3L); f3.setTenantId(1L); f3.setName("上市状态"); f3.setSorting(3); f3.setStatus(1); focusMapper.insert(f3);
        Focus f4 = new Focus(); f4.setId(4L); f4.setTenantId(1L); f4.setName("科技资质"); f4.setSorting(4); f4.setStatus(1); focusMapper.insert(f4);

        addFocusItem(1L,  1L, "世界500强",          1);
        addFocusItem(2L,  1L, "中国500强",          2);
        addFocusItem(3L,  1L, "规模以上",          3);
        addFocusItem(4L,  2L, "上市公司",          1);
        addFocusItem(5L,  2L, "独角兽",            2);
        addFocusItem(6L,  2L, "瞪羚企业",          3);
        addFocusItem(7L,  3L, "已上市",            1);
        addFocusItem(8L,  3L, "未上市",            2);
        addFocusItem(9L,  4L, "高新技术企业",       1);
        addFocusItem(10L, 4L, "专精特新",          2);
        addFocusItem(11L, 4L, "技术先进型服务企业", 3);
    }

    private void addFocusItem(Long id, Long focusId, String name, int sorting) {
        FocusItem i = new FocusItem(); i.setId(id); i.setTenantId(1L); i.setFocusId(focusId);
        i.setName(name); i.setSorting(sorting); i.setStatus(1);
        focusItemMapper.insert(i);
    }

    private void seedNationalEconomyAndRegType() {
        addNationalEconomy(1L, "C",   "制造业",                              null,  1, 1);
        addNationalEconomy(2L, "C39", "计算机、通信和其他电子设备制造业",         "C",   2, 1);
        addNationalEconomy(3L, "C36", "汽车制造业",                            "C",   2, 2);
        addNationalEconomy(4L, "C38", "电气机械和器材制造业",                     "C",   2, 3);
        addNationalEconomy(5L, "I",   "信息传输、软件和信息技术服务业",                null,  1, 2);
        addNationalEconomy(6L, "I64", "互联网和相关服务",                        "I",   2, 1);
        addNationalEconomy(7L, "I65", "软件和信息技术服务业",                       "I",   2, 2);
        addNationalEconomy(8L, "F",   "批发和零售业",                            null,  1, 3);
        addNationalEconomy(9L, "J",   "金融业",                                 null,  1, 4);

        addRegType(1L, "1100", "有限责任公司",                          null,  1);
        addRegType(2L, "1110", "有限责任公司(自然人投资或控股)",     "1100", 1);
        addRegType(3L, "1120", "有限责任公司(国有独资)",                "1100", 2);
        addRegType(4L, "1130", "有限责任公司(外商投资)",                "1100", 3);
        addRegType(5L, "1200", "股份有限公司",                          null,  2);
        addRegType(6L, "1210", "股份有限公司(上市)",                  "1200", 1);
        addRegType(7L, "1219", "股份有限公司(非上市)",                "1200", 2);
        addRegType(8L, "2100", "个人独资企业",                          null,  3);
        addRegType(9L, "2200", "合伙企业",                              null,  4);
    }

    private void addNationalEconomy(Long id, String code, String name, String parentCode, Integer level, int sortOrder) {
        NationalEconomy e = new NationalEconomy(); e.setId(id); e.setTenantId(1L);
        e.setCode(code); e.setName(name); e.setParentCode(parentCode);
        e.setLevel(level); e.setSortOrder(sortOrder); e.setStatus(1); e.setCreateBy(CB);
        nationalEconomyMapper.insert(e);
    }

    private void addRegType(Long id, String code, String name, String parentCode, int sortOrder) {
        EntRegType e = new EntRegType(); e.setId(id); e.setTenantId(1L);
        e.setCode(code); e.setName(name); e.setParentCode(parentCode);
        e.setSortOrder(sortOrder); e.setStatus(1); e.setCreateBy(CB);
        enterpriseRegTypeMapper.insert(e);
    }

    private void seedEnterprises() {
        addEnt(1L, "华为技术有限公司", "华为", "Huawei Technologies Co., Ltd.",
            "914403001921810943", "通信设备", "C", "C39", "C391", "C3911",
            "任正非", "有限责任公司(自然人投资或控股)", "4044093.8266万元",
            LocalDateTime.of(1987, 9, 15, 0, 0), LocalDateTime.of(2024, 4, 10, 0, 0),
            "广东省", "深圳市", "龙岗区", "深圳市龙岗区坂田街道华为基地", "深圳市市场监督管理局",
            156000, "程控交换机、传输设备、数据通信设备、宽带多媒体设备、电源、无线通信设备、微电子产品、软件、系统集成工程、计算机及配套设备、终端设备及相关通信信息产品、化工产品(不含危险品)、光机电一体化产品、电子出版物、出口自研产品等",
            "400-822-9999", "support@huawei.com", "https://www.huawei.com",
            "世界500强,高新技术,通信设备", null, null, null, 1);

        addEnt(2L, "腾讯科技(深圳)有限公司", "腾讯", "Tencent Technology (Shenzhen) Co., Ltd.",
            "9144030071526726XG", "互联网服务", "I", "I64", "I641", "I6410",
            "马化腾", "有限责任公司(外商投资)", "2000000.00万元",
            LocalDateTime.of(1998, 11, 11, 0, 0), LocalDateTime.of(2024, 3, 15, 0, 0),
            "广东省", "深圳市", "南山区", "深圳市南山区高新区科技中一路腾讯大厦35层", "深圳市市场监督管理局",
            112000, "计算机软硬件的技术开发、销售技术服务;计算机及相关设备的安装、维护、租赁;计算机网络工程的设计、施工;经营进出口业务;增值电信业务;信息服务业务",
            "0755-86013388", "service@tencent.com", "https://www.tencent.com",
            "中国500强,互联网头部,高新技术", "00700", "腾讯控股", "港股", 2);

        addEnt(3L, "比亚迪股份有限公司", "比亚迪", "BYD Company Limited",
            "91440300192172583R", "新能源汽车", "C", "C36", "C361", "C3610",
            "王传福", "股份有限公司(上市)", "291114.2855万元",
            LocalDateTime.of(1995, 2, 10, 0, 0), LocalDateTime.of(2024, 5, 8, 0, 0),
            "广东省", "深圳市", "坪山区", "深圳市坪山区比亚迪路3009号", "深圳市市场监督管理局",
            703500, "汽车(含小轿车)的研发、生产、销售及售后服务;锂离子电池、镍镉电池、镍氢电池、动力电池、储能电池及电池零配件的研发、生产、销售;塑胶制品、五金制品的研发、生产、销售;模具设计与制造;技术咨询和服务",
            "0755-89888888", "ir@byd.com", "https://www.byd.com",
            "中国500强,新能源汽车,高新技术", "002594", "比亚迪", "深A", 3);

        addEnt(4L, "阿里巴巴(中国)网络技术有限公司", "阿里", "Alibaba (China) Network Technology Co., Ltd.",
            "91330100799655058B", "电子商务", "I", "I64", "I641", "I6410",
            "戴珊", "有限责任公司(外商投资)", "1590000.00万元",
            LocalDateTime.of(1999, 9, 9, 0, 0), LocalDateTime.of(2024, 2, 20, 0, 0),
            "浙江省", "杭州市", "滨江区", "杭州市滨江区网商路699号", "浙江省市场监督管理局",
            124000, "增值电信业务;计算机软硬件、网络技术、电子商务技术开发、技术服务、技术咨询、技术转让;计算机软硬件的销售;设计、制作、代理、发布国内广告",
            "0571-85022088", "ir@alibaba-inc.com", "https://www.alibabagroup.com",
            "中国500强,电商龙头,高新技术", "09988", "阿里巴巴-W", "港股", 4);

        addEnt(5L, "宁德时代新能源科技股份有限公司", "宁德时代", "Contemporary Amperex Technology Co., Limited",
            "91350900MA32DHA7XR", "动力电池", "C", "C38", "C384", "C3840",
            "曾毓群", "股份有限公司(上市)", "439880.9536万元",
            LocalDateTime.of(2011, 12, 16, 0, 0), LocalDateTime.of(2024, 6, 12, 0, 0),
            "福建省", "宁德市", "蕉城区", "宁德市蕉城区漳湾镇新港路2号", "宁德市市场监督管理局",
            138000, "锂离子电池、锂聚合物电池、燃料电池、动力电池、超大容量储能电池及电池零配件的研发、生产、销售和服务;锂电池及相关产品的技术服务、测试服务以及咨询服务",
            "0593-2583666", "catl@catlbattery.com", "https://www.catl.com",
            "独角兽,动力电池龙头,高新技术", "300750", "宁德时代", "创业板", 5);
    }

    private void addEnt(Long id, String name, String alias, String engName, String credit,
                         String industry, String cat, String big, String mid, String small,
                         String legalRep, String orgType, String regCap,
                         LocalDateTime est, LocalDateTime approved,
                         String base, String city, String district, String location, String institute,
                         int staff, String scope, String phone, String email, String web,
                         String tags, String bondNum, String bondName, String bondType, int sortOrder) {
        Enterprise e = new Enterprise();
        e.setId(id); e.setTenantId(1L);
        e.setName(name); e.setAlias(alias); e.setEngName(engName);
        e.setTaxNumber(credit); e.setCreditCode(credit);
        e.setIndustry(industry); e.setCategory(cat);
        e.setCategoryBig(big); e.setCategoryMiddle(mid); e.setCategorySmall(small);
        e.setLegalPersonName(legalRep); e.setType(1); e.setCompanyOrgType(orgType);
        e.setRegCapital(regCap); e.setRegCapitalCurrency("CNY");
        e.setActualCapital(regCap); e.setActualCapitalCurrency("CNY");
        e.setEstiblishTime(est); e.setFromTime(est); e.setApprovedTime(approved);
        e.setBase(base); e.setCity(city); e.setDistrict(district);
        e.setRegLocation(location); e.setRegInstitute(institute);
        e.setRegStatus("存续"); e.setStaffNumRange("10000人以上"); e.setSocialStaffNum(staff);
        e.setBusinessScope(scope);
        e.setPhoneNumber(phone); e.setEmail(email); e.setWebsiteList(web);
        e.setTags(tags);
        e.setBondNum(bondNum); e.setBondName(bondName); e.setBondType(bondType);
        e.setStatus(1); e.setIsSync(0); e.setIsFill(0);
        e.setCreateBy(CB);
        enterpriseMapper.insert(e);
    }

    private void seedAddresses() {
        addAddress(1L, "广东省", "深圳市", "龙岗区", "深圳市龙岗区坂田街道华为基地");
        addAddress(2L, "广东省", "深圳市", "南山区", "深圳市南山区高新区科技中一路腾讯大厦");
        addAddress(3L, "广东省", "深圳市", "坪山区", "深圳市坪山区比亚迪路3009号");
        addAddress(4L, "浙江省", "杭州市", "滨江区", "杭州市滨江区网商路699号");
        addAddress(5L, "福建省", "宁德市", "蕉城区", "宁德市蕉城区漳湾镇新港路2号");
    }

    private void addAddress(Long entId, String base, String city, String district, String location) {
        EnterpriseAddress a = new EnterpriseAddress();
        a.setId(entId); a.setTenantId(1L); a.setEnterpriseId(entId);
        a.setBase(base); a.setCity(city); a.setDistrict(district); a.setRegLocation(location);
        enterpriseAddressMapper.insert(a);
    }

    private void seedIntroductions() {
        addIntro(1L, "ICT基础设施、智能终端、云服务", "有限责任公司(自然人投资或控股)", "世界500强,高新技术", 95, 156000);
        addIntro(2L, "社交、游戏、金融科技、云",   "有限责任公司(外商投资)",     "中国500强,互联网头部", 96, 112000);
        addIntro(3L, "新能源汽车、动力电池、电子", "股份有限公司(上市)",         "中国500强,新能源汽车", 88, 703500);
        addIntro(4L, "电商、云计算、数字媒体",     "有限责任公司(外商投资)",     "中国500强,电商龙头",   92, 124000);
        addIntro(5L, "动力电池、储能电池",         "股份有限公司(上市)",         "独角兽,动力电池龙头",  90, 138000);
    }

    private void addIntro(Long entId, String scope, String orgType, String tags, int score, int staff) {
        EnterpriseIntroduction i = new EnterpriseIntroduction();
        i.setId(entId); i.setTenantId(1L); i.setEnterpriseId(entId);
        i.setBusinessScope(scope); i.setType(1); i.setCompanyOrgType(orgType);
        i.setTags(tags); i.setPercentileScore(score);
        i.setStaffNumRange("10000人以上"); i.setSocialStaffNum(staff);
        enterpriseIntroductionMapper.insert(i);
    }

    private void seedRegisters() {
        addReg(1L, "4403011029903889", "4044093.8266万元",  "深圳市市场监督管理局",   2024, 4, 10);
        addReg(2L, "440301501124589",  "2000000.00万元",     "深圳市市场监督管理局",   2024, 3, 15);
        addReg(3L, "440301102920209",  "291114.2855万元",    "深圳市市场监督管理局",   2024, 5, 8);
        addReg(4L, "330100000015384",  "1590000.00万元",     "浙江省市场监督管理局",   2024, 2, 20);
        addReg(5L, "350900100006128",  "439880.9536万元",    "宁德市市场监督管理局",   2024, 6, 12);
    }

    private void addReg(Long entId, String regNo, String actualCap, String institute, int y, int m, int d) {
        EnterpriseRegister r = new EnterpriseRegister();
        r.setId(entId); r.setTenantId(1L); r.setEnterpriseId(entId);
        r.setRegNumber(regNo); r.setRegCapitalCurrency("CNY");
        r.setActualCapital(actualCap); r.setActualCapitalCurrency("CNY");
        r.setRegInstitute(institute);
        r.setApprovedTime(LocalDateTime.of(y, m, d, 0, 0));
        enterpriseRegisterMapper.insert(r);
    }

    private void seedTags() {
        addTag(1L, 1L, "世界500强",   "#E74C3C", 1);
        addTag(2L, 1L, "高新技术",   "#3498DB", 2);
        addTag(3L, 2L, "中国500强",   "#E74C3C", 1);
        addTag(4L, 2L, "互联网头部",  "#2ECC71", 2);
        addTag(5L, 3L, "中国500强",   "#E74C3C", 1);
        addTag(6L, 3L, "新能源汽车",  "#F39C12", 2);
        addTag(7L, 4L, "中国500强",   "#E74C3C", 1);
        addTag(8L, 4L, "电商龙头",   "#9B59B6", 2);
        addTag(9L, 5L, "独角兽",     "#1ABC9C", 1);
        addTag(10L, 5L, "动力电池龙头", "#E67E22", 2);
    }

    private void addTag(Long id, Long entId, String name, String color, int order) {
        EnterpriseTag t = new EnterpriseTag();
        t.setId(id); t.setTenantId(1L); t.setEnterpriseId(entId);
        t.setTagName(name); t.setTagColor(color); t.setSortOrder(order);
        t.setCreateBy(CB);
        enterpriseTagMapper.insert(t);
    }

    private void seedIndustries() {
        addIndustry(1L, "C3911", "制造业", "计算机、通信和其他电子设备制造业", "通信设备制造", "通信系统设备制造");
        addIndustry(2L, "I6410", "信息传输、软件和信息技术服务业", "互联网和相关服务", "互联网接入及相关服务", "互联网接入及相关服务");
        addIndustry(3L, "C3610", "制造业", "汽车制造业", "汽车整车制造", "汽车整车制造");
        addIndustry(4L, "C3840", "制造业", "电气机械和器材制造业", "电池制造", "锂离子电池制造");
        addIndustry(5L, "I6420", "信息传输、软件和信息技术服务业", "互联网和相关服务", "互联网安全服务", "互联网安全服务");
    }

    private void addIndustry(Long id, String code, String cat, String big, String mid, String small) {
        EnterpriseIndustry i = new EnterpriseIndustry();
        i.setId(id); i.setTenantId(1L);
        i.setCode(code); i.setCategory(cat); i.setCategoryBig(big);
        i.setCategoryMiddle(mid); i.setCategorySmall(small);
        i.setStatus(1);
        enterpriseIndustryMapper.insert(i);
    }

    private void seedEnterpriseFocus() {
        addEntFocus(1L, 1L, 1L, "企业规模", "1", "世界500强");
        addEntFocus(2L, 2L, 1L, "企业规模", "2", "中国500强");
        addEntFocus(3L, 3L, 1L, "企业规模", "2", "中国500强");
        addEntFocus(4L, 4L, 1L, "企业规模", "2", "中国500强");
        addEntFocus(5L, 5L, 2L, "企业类型", "5", "独角兽");
    }

    private void addEntFocus(Long id, Long entId, Long focusId, String focusName, String items, String itemNames) {
        EnterpriseFocus f = new EnterpriseFocus();
        f.setId(id); f.setTenantId(1L); f.setEnterpriseId(entId);
        f.setFocusId(focusId); f.setFocusName(focusName);
        f.setFocusItems(items); f.setFocusItemNames(itemNames);
        f.setStatus(1);
        enterpriseFocusMapper.insert(f);
    }

    private void seedCustomerInfo() {
        addCust(1L, "CUST-HW-001",   50000.00,  40440938266.00,  800000000.00, 100000000.00, 95000, 35000, 25000, 8000, 12000, 18000, 5, 4, 6, "深圳龙岗坂田基地A栋", 2, 2, 3, "建议园区配套研发实验室");
        addCust(2L, "CUST-TX-001",   35000.00, 20000000000.00,  500000000.00,  80000000.00, 65000, 28000, 18000, 5500,  9500, 12000, 5, 4, 6, "深圳南山腾讯大厦",       2, 2, 3, "建议园区配套数据中心");
        addCust(3L, "CUST-BYD-001",  80000.00,  2911142855.00,  300000000.00,  50000000.00, 42000, 15000, 12000, 4000,  8000,  5000, 4, 4, 5, "深圳坪山比亚迪总部",   3, 2, 3, "建议园区配套电池研发实验室");
        addCust(4L, "CUST-ALB-001",  60000.00, 15900000000.00,  700000000.00,  90000000.00, 38000, 12000,  9000, 3000,  7000,  7000, 5, 4, 6, "杭州滨江阿里中心",       2, 2, 3, "建议园区配套大型数据中心");
        addCust(5L, "CUST-CATL-001", 40000.00,  4398809536.00,  250000000.00,  30000000.00, 18500,  8500,  5000, 1500,  2500,  1000, 4, 4, 5, "宁德蕉城基地",          3, 2, 3, "建议园区配套电池中试线");
    }

    private void addCust(Long id, String code, double area, double regMoney, double invest, double financing,
                          int ip, int invention, int utility, int design, int trademark, int sw,
                          int indField, int bizIncome, int taxLevel,
                          String address, int structLoad, int floorHeight, int capacitance,
                          String suggest) {
        CustomerInformation c = new CustomerInformation();
        c.setId(id); c.setTenantId(1L); c.setParkId(1L);
        c.setCode(code); c.setCustomerType(3);
        c.setArea(new BigDecimal(String.valueOf(area)));
        c.setSettleAddress(address);
        c.setName(entName(id)); c.setPhone(entPhone(id));
        c.setManagerPhone("1380000000" + id); c.setEmail(entEmail(id));
        c.setRegistTime(entRegTime(id));
        c.setRegistMoney(new BigDecimal(String.valueOf(regMoney)));
        c.setRentalStandard(entRental(id));
        c.setIndustrialField(indField);
        c.setMainBusiness(entMainBiz(id));
        c.setCompanyStrengths(entStrengths(id));
        c.setValidIntellectualProperty(ip);
        c.setInventionPatents(invention); c.setUtilityModelPatent(utility);
        c.setIndustrialDesignPatents(design); c.setTrademark(trademark); c.setSoftwareCopyright(sw);
        c.setBusinessIncome(bizIncome); c.setLastYearTax(taxLevel);
        c.setTotalInvestmentAmount(new BigDecimal(String.valueOf(invest)));
        c.setTotalFinancingAmount(new BigDecimal(String.valueOf(financing)));
        c.setSuggest(suggest);
        c.setStructuralLoad(structLoad); c.setFloorHeight(floorHeight); c.setCapacitance(capacitance);
        c.setStatus(1);
        // 关联 enterpriseId 在 enterprise 之后插入
        c.setEnterpriseId(id);
        customerInformationMapper.insert(c);
    }

    private String entName(Long id) {
        switch (id.intValue()) {
            case 1: return "任正非"; case 2: return "马化腾"; case 3: return "王传福";
            case 4: return "戴珊"; case 5: return "曾毓群"; default: return "";
        }
    }
    private String entPhone(Long id) {
        switch (id.intValue()) {
            case 1: return "400-822-9999"; case 2: return "0755-86013388"; case 3: return "0755-89888888";
            case 4: return "0571-85022088"; case 5: return "0593-2583666"; default: return "";
        }
    }
    private String entEmail(Long id) {
        switch (id.intValue()) {
            case 1: return "support@huawei.com"; case 2: return "service@tencent.com"; case 3: return "ir@byd.com";
            case 4: return "ir@alibaba-inc.com"; case 5: return "catl@catlbattery.com"; default: return "";
        }
    }
    private LocalDateTime entRegTime(Long id) {
        switch (id.intValue()) {
            case 1: return LocalDateTime.of(1987, 9, 15, 0, 0);
            case 2: return LocalDateTime.of(1998, 11, 11, 0, 0);
            case 3: return LocalDateTime.of(1995, 2, 10, 0, 0);
            case 4: return LocalDateTime.of(1999, 9, 9, 0, 0);
            case 5: return LocalDateTime.of(2011, 12, 16, 0, 0);
            default: return LocalDateTime.now();
        }
    }
    private String entRental(Long id) {
        switch (id.intValue()) {
            case 1: return "120元/m²/月"; case 2: return "150元/m²/月"; case 3: return "90元/m²/月";
            case 4: return "140元/m²/月"; case 5: return "100元/m²/月"; default: return "";
        }
    }
    private String entMainBiz(Long id) {
        switch (id.intValue()) {
            case 1: return "ICT基础设施、智能终端、云服务";
            case 2: return "社交、游戏、金融科技、云";
            case 3: return "新能源汽车、动力电池、电子";
            case 4: return "电商、云计算、数字媒体";
            case 5: return "动力电池、储能电池";
            default: return "";
        }
    }
    private String entStrengths(Long id) {
        switch (id.intValue()) {
            case 1: return "1,2,9"; case 2: return "1,2,9"; case 3: return "1,2";
            case 4: return "1,2,9"; case 5: return "1,5,9"; default: return "";
        }
    }

    private void seedCloudData() {
        addCloud(1L,  1L, "华为技术有限公司",                  "business_risk",     "{\"event\":\"无重大经营风险\",\"score\":\"AAA\"}");
        addCloud(2L,  2L, "腾讯科技(深圳)有限公司",            "business_risk",     "{\"event\":\"无重大经营风险\",\"score\":\"AAA\"}");
        addCloud(3L,  3L, "比亚迪股份有限公司",               "business_risk",     "{\"event\":\"1次环保处罚(已整改)\",\"score\":\"AA\"}");
        addCloud(4L,  4L, "阿里巴巴(中国)网络技术有限公司",     "business_risk",     "{\"event\":\"1次反垄断处罚(已缴纳)\",\"score\":\"A\"}");
        addCloud(5L,  5L, "宁德时代新能源科技股份有限公司",     "business_risk",     "{\"event\":\"无重大经营风险\",\"score\":\"AAA\"}");
        addCloud(6L,  1L, "华为技术有限公司",                  "judicial_risk",     "{\"event\":\"0条\",\"lawsuit\":0,\"execution\":0,\"dishonest\":0}");
        addCloud(7L,  2L, "腾讯科技(深圳)有限公司",            "judicial_risk",     "{\"event\":\"12条开庭公告\",\"lawsuit\":12}");
        addCloud(8L,  3L, "比亚迪股份有限公司",               "judicial_risk",     "{\"event\":\"5条开庭公告\",\"lawsuit\":5}");
        addCloud(9L,  4L, "阿里巴巴(中国)网络技术有限公司",     "judicial_risk",     "{\"event\":\"0条\",\"lawsuit\":0}");
        addCloud(10L, 5L, "宁德时代新能源科技股份有限公司",     "judicial_risk",     "{\"event\":\"3条开庭公告\",\"lawsuit\":3}");
        addCloud(11L, 1L, "华为技术有限公司",                  "business_situation","{\"taxCredit\":\"A\",\"creditRating\":\"AAA\",\"recruitCount\":3200,\"bidCount\":158}");
        addCloud(12L, 2L, "腾讯科技(深圳)有限公司",            "business_situation","{\"taxCredit\":\"A\",\"creditRating\":\"AAA\",\"recruitCount\":8500,\"bidCount\":42}");
        addCloud(13L, 3L, "比亚迪股份有限公司",               "business_situation","{\"taxCredit\":\"A\",\"creditRating\":\"AA\",\"recruitCount\":28000,\"bidCount\":356}");
        addCloud(14L, 4L, "阿里巴巴(中国)网络技术有限公司",     "business_situation","{\"taxCredit\":\"A\",\"creditRating\":\"AA\",\"recruitCount\":15000,\"bidCount\":28}");
        addCloud(15L, 5L, "宁德时代新能源科技股份有限公司",     "business_situation","{\"taxCredit\":\"A\",\"creditRating\":\"AAA\",\"recruitCount\":18000,\"bidCount\":124}");
        addCloud(16L, 1L, "华为技术有限公司",                  "ent_detail",        "{\"patentCount\":35000,\"trademarkCount\":12000,\"softwareCopyright\":18000}");
        addCloud(17L, 2L, "腾讯科技(深圳)有限公司",            "ent_detail",        "{\"patentCount\":28000,\"trademarkCount\":9500,\"softwareCopyright\":12000}");
        addCloud(18L, 3L, "比亚迪股份有限公司",               "ent_detail",        "{\"patentCount\":15000,\"trademarkCount\":8000,\"softwareCopyright\":5000}");
        addCloud(19L, 4L, "阿里巴巴(中国)网络技术有限公司",     "ent_detail",        "{\"patentCount\":12000,\"trademarkCount\":7000,\"softwareCopyright\":7000}");
        addCloud(20L, 5L, "宁德时代新能源科技股份有限公司",     "ent_detail",        "{\"patentCount\":8500,\"trademarkCount\":2500,\"softwareCopyright\":1000}");
        addCloud(21L, 1L, "华为技术有限公司",                  "knowledge",         "{\"title\":\"华为2024年报\",\"summary\":\"营收7042亿元\"}");
        addCloud(22L, 2L, "腾讯科技(深圳)有限公司",            "knowledge",         "{\"title\":\"腾讯2024年报\",\"summary\":\"营收6098亿元\"}");
        addCloud(23L, 3L, "比亚迪股份有限公司",               "knowledge",         "{\"title\":\"比亚迪2024销量\",\"summary\":\"新能源车427万辆\"}");
        addCloud(24L, 4L, "阿里巴巴(中国)网络技术有限公司",     "knowledge",         "{\"title\":\"阿里2024财年\",\"summary\":\"营收9411亿元\"}");
        addCloud(25L, 5L, "宁德时代新能源科技股份有限公司",     "knowledge",         "{\"title\":\"宁德2024业绩\",\"summary\":\"营收4009亿元\"}");
    }

    private void addCloud(Long id, Long entId, String name, String category, String content) {
        CloudData d = new CloudData();
        d.setId(id); d.setTenantId(1L); d.setEnterpriseId(entId);
        d.setEnterpriseName(name); d.setCategory(category);
        d.setDataContent(content); d.setDataYear("2024");
        d.setSortOrder(1); d.setStatus(1); d.setCreateBy(CB);
        cloudDataMapper.insert(d);
    }

    private void seedOverview() {
        addOverview(1L, 1L, "华为技术有限公司",                  "4044093.83万元", "10258亿元", "7042亿元",  207000, 35000, 12000, 18000, 0,   158, "[{\"shareholder\":\"任正非\",\"ratio\":\"0.65%\"}]",  "{\"world500\":true,\"listing\":\"未上市\"}", "世界500强");
        addOverview(2L, 2L, "腾讯科技(深圳)有限公司",            "2000000.00万元", "9680亿元",  "6098亿元",  112000, 28000,  9500, 12000, 12,  42,  "[{\"shareholder\":\"马化腾\",\"ratio\":\"8.41%\"}]",  "{\"world500\":true,\"listing\":\"港股00700\"}", "世界500强,港股");
        addOverview(3L, 3L, "比亚迪股份有限公司",               "291114.29万元",  "7632亿元",  "6023亿元",  703500, 15000,  8000,  5000, 5,   356, "[{\"shareholder\":\"王传福\",\"ratio\":\"17.64%\"}]", "{\"world500\":true,\"listing\":\"A+H 002594\"}",   "世界500强,A+H");
        addOverview(4L, 4L, "阿里巴巴(中国)网络技术有限公司",     "1590000.00万元", "13020亿元", "9411亿元",  219260, 12000,  7000,  7000, 1,   28,  "[{\"shareholder\":\"软银\",\"ratio\":\"13.97%\"}]",   "{\"world500\":true,\"listing\":\"港股09988\"}",   "世界500强,港股+美股");
        addOverview(5L, 5L, "宁德时代新能源科技股份有限公司",     "439880.95万元",  "6276亿元",  "4009亿元",  138000,  8500,  2500,  1000, 3,   124, "[{\"shareholder\":\"曾毓群\",\"ratio\":\"23.27%\"}]","{\"world500\":true,\"listing\":\"创业板300750\"}","世界500强,创业板");
    }

    private void addOverview(Long id, Long entId, String name, String regCap, String assets, String revenue,
                              int emp, int patent, int tm, int cp, int risk, int bid,
                              String equity, String overview, String remark) {
        EnterpriseOverview o = new EnterpriseOverview();
        o.setId(id); o.setTenantId(1L); o.setEnterpriseId(entId); o.setEnterpriseName(name);
        o.setRegCapital(regCap); o.setTotalAssets(assets); o.setAnnualRevenue(revenue);
        o.setEmployeeCount(emp); o.setPatentCount(patent); o.setTrademarkCount(tm);
        o.setCopyrightCount(cp); o.setRiskCount(risk); o.setBidCount(bid);
        o.setEquityStructureJson(equity); o.setOverviewJson(overview);
        o.setSortOrder(id.intValue()); o.setStatus(1); o.setRemark(remark);
        o.setCreateBy(CB);
        enterpriseOverviewMapper.insert(o);
    }
}
