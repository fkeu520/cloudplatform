-- =============================================================================
-- V64__seed_mock_data.sql
-- park-enterprise Phase 3 - 真实企业模拟数据
-- 5 家中国头部企业: 华为/腾讯/比亚迪/阿里巴巴/宁德时代
-- 覆盖所有表, 用于演示和测试 (3-5 条/企业)
--
-- 重要: 列名严格匹配 V53-V58 实际 schema, 不再使用 csyh 原始表结构
-- 失败教训: 上一版错用 sys_enterprise_introduction.content (V55 无此列)
-- =============================================================================

-- =============================================================================
-- 1. 参考数据: 关注标签 (sys_focus)
-- =============================================================================
INSERT INTO sys_focus (id, tenant_id, park_id, name, sorting, status, create_by, create_time, deleted) VALUES
    (1, 1, NULL, '企业规模',  1, 1, 'system', '2026-07-01 10:00:00', 0),
    (2, 1, NULL, '企业类型',  2, 1, 'system', '2026-07-01 10:00:00', 0),
    (3, 1, NULL, '上市状态',  3, 1, 'system', '2026-07-01 10:00:00', 0),
    (4, 1, NULL, '科技资质',  4, 1, 'system', '2026-07-01 10:00:00', 0);

INSERT INTO sys_focus_item (id, tenant_id, park_id, focus_id, name, sorting, status, create_by, create_time, deleted) VALUES
    (1,  1, NULL, 1, '世界500强',            1, 1, 'system', '2026-07-01 10:00:00', 0),
    (2,  1, NULL, 1, '中国500强',            2, 1, 'system', '2026-07-01 10:00:00', 0),
    (3,  1, NULL, 1, '规模以上',            3, 1, 'system', '2026-07-01 10:00:00', 0),
    (4,  1, NULL, 2, '上市公司',            1, 1, 'system', '2026-07-01 10:00:00', 0),
    (5,  1, NULL, 2, '独角兽',              2, 1, 'system', '2026-07-01 10:00:00', 0),
    (6,  1, NULL, 2, '瞪羚企业',            3, 1, 'system', '2026-07-01 10:00:00', 0),
    (7,  1, NULL, 3, '已上市',              1, 1, 'system', '2026-07-01 10:00:00', 0),
    (8,  1, NULL, 3, '未上市',              2, 1, 'system', '2026-07-01 10:00:00', 0),
    (9,  1, NULL, 4, '高新技术企业',          1, 1, 'system', '2026-07-01 10:00:00', 0),
    (10, 1, NULL, 4, '专精特新',            2, 1, 'system', '2026-07-01 10:00:00', 0),
    (11, 1, NULL, 4, '技术先进型服务企业',  3, 1, 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 2. 参考数据: 国民经济行业分类 (sys_enterprise_national_economy)
-- =============================================================================
INSERT INTO sys_enterprise_national_economy (id, tenant_id, code, name, parent_code, level, sort_order, status, create_by, create_time, deleted) VALUES
    (1, 1, 'C',   '制造业',                              NULL, 1, 1, 1, 'system', '2026-07-01 10:00:00', 0),
    (2, 1, 'C39', '计算机、通信和其他电子设备制造业',         'C',  2, 1, 1, 'system', '2026-07-01 10:00:00', 0),
    (3, 1, 'C36', '汽车制造业',                            'C',  2, 2, 1, 'system', '2026-07-01 10:00:00', 0),
    (4, 1, 'C38', '电气机械和器材制造业',                     'C',  2, 3, 1, 'system', '2026-07-01 10:00:00', 0),
    (5, 1, 'I',   '信息传输、软件和信息技术服务业',                NULL, 1, 2, 1, 'system', '2026-07-01 10:00:00', 0),
    (6, 1, 'I64', '互联网和相关服务',                        'I',  2, 1, 1, 'system', '2026-07-01 10:00:00', 0),
    (7, 1, 'I65', '软件和信息技术服务业',                       'I',  2, 2, 1, 'system', '2026-07-01 10:00:00', 0),
    (8, 1, 'F',   '批发和零售业',                            NULL, 1, 3, 1, 'system', '2026-07-01 10:00:00', 0),
    (9, 1, 'J',   '金融业',                                 NULL, 1, 4, 1, 'system', '2026-07-01 10:00:00', 0);

INSERT INTO sys_enterprise_reg_type (id, tenant_id, code, name, parent_code, sort_order, status, create_by, create_time, deleted) VALUES
    (1, 1, '1100', '有限责任公司',                          NULL,    1, 1, 'system', '2026-07-01 10:00:00', 0),
    (2, 1, '1110', '有限责任公司(自然人投资或控股)',     '1100', 1, 1, 'system', '2026-07-01 10:00:00', 0),
    (3, 1, '1120', '有限责任公司(国有独资)',                '1100', 2, 1, 'system', '2026-07-01 10:00:00', 0),
    (4, 1, '1130', '有限责任公司(外商投资)',                '1100', 3, 1, 'system', '2026-07-01 10:00:00', 0),
    (5, 1, '1200', '股份有限公司',                          NULL,    2, 1, 'system', '2026-07-01 10:00:00', 0),
    (6, 1, '1210', '股份有限公司(上市)',                  '1200', 1, 1, 'system', '2026-07-01 10:00:00', 0),
    (7, 1, '1219', '股份有限公司(非上市)',                '1200', 2, 1, 'system', '2026-07-01 10:00:00', 0),
    (8, 1, '2100', '个人独资企业',                          NULL,    3, 1, 'system', '2026-07-01 10:00:00', 0),
    (9, 1, '2200', '合伙企业',                              NULL,    4, 1, 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 3. 主数据: 5 家真实企业 (sys_enterprise, ID 1-5)
-- 列严格匹配 V53 schema
-- =============================================================================
INSERT INTO sys_enterprise (
    id, tenant_id,
    name, alias, eng_name, tax_number, credit_code,
    industry, category, category_big, category_middle, category_small,
    legal_person_name, type, company_org_type,
    reg_capital, reg_capital_currency, actual_capital, actual_capital_currency,
    estiblish_time, from_time, approved_time,
    base, city, district, reg_location, reg_institute,
    reg_status, staff_num_range, social_staff_num, business_scope,
    phone_number, email, website_list,
    tags, percentile_score,
    bond_num, bond_name, bond_type,
    status, logo, origin_id, is_sync, is_fill,
    accessory, org_file_id, executive_file_id, investor_file_id,
    create_by, create_time, deleted
) VALUES
-- 1. 华为技术有限公司
(1, 1,
 '华为技术有限公司', '华为', 'Huawei Technologies Co., Ltd.', '914403001921810943', '914403001921810943',
 '通信设备', 'C', 'C39', 'C391', 'C3911',
 '任正非', 1, '有限责任公司(自然人投资或控股)',
 '4044093.8266万元', 'CNY', '4044093.8266万元', 'CNY',
 '1987-09-15 00:00:00', '1987-09-15 00:00:00', '2024-04-10 00:00:00',
 '广东省', '深圳市', '龙岗区', '深圳市龙岗区坂田街道华为基地', '深圳市市场监督管理局',
 '存续', '10000人以上', 156000, '程控交换机、传输设备、数据通信设备、宽带多媒体设备、电源、无线通信设备、微电子产品、软件、系统集成工程、计算机及配套设备、终端设备及相关通信信息产品、化工产品(不含危险品)、光机电一体化产品、电子出版物、出口自研产品等',
 '400-822-9999', 'support@huawei.com', 'https://www.huawei.com',
 '世界500强,高新技术,通信设备', NULL,
 NULL, NULL, NULL,
 1, NULL, NULL, 0, 0,
 NULL, NULL, NULL, NULL,
 'system', '2026-07-01 10:00:00', 0),

-- 2. 腾讯科技(深圳)有限公司
(2, 1,
 '腾讯科技(深圳)有限公司', '腾讯', 'Tencent Technology (Shenzhen) Co., Ltd.', '9144030071526726XG', '9144030071526726XG',
 '互联网服务', 'I', 'I64', 'I641', 'I6410',
 '马化腾', 1, '有限责任公司(外商投资)',
 '2000000.00万元', 'CNY', '2000000.00万元', 'CNY',
 '1998-11-11 00:00:00', '1998-11-11 00:00:00', '2024-03-15 00:00:00',
 '广东省', '深圳市', '南山区', '深圳市南山区高新区科技中一路腾讯大厦35层', '深圳市市场监督管理局',
 '存续', '10000人以上', 112000, '计算机软硬件的技术开发、销售技术服务;计算机及相关设备的安装、维护、租赁;计算机网络工程的设计、施工;经营进出口业务;增值电信业务;信息服务业务',
 '0755-86013388', 'service@tencent.com', 'https://www.tencent.com',
 '中国500强,互联网头部,高新技术', NULL,
 '00700', '腾讯控股', '港股',
 1, NULL, NULL, 0, 0,
 NULL, NULL, NULL, NULL,
 'system', '2026-07-01 10:00:00', 0),

-- 3. 比亚迪股份有限公司
(3, 1,
 '比亚迪股份有限公司', '比亚迪', 'BYD Company Limited', '91440300192172583R', '91440300192172583R',
 '新能源汽车', 'C', 'C36', 'C361', 'C3610',
 '王传福', 1, '股份有限公司(上市)',
 '291114.2855万元', 'CNY', '291114.2855万元', 'CNY',
 '1995-02-10 00:00:00', '1995-02-10 00:00:00', '2024-05-08 00:00:00',
 '广东省', '深圳市', '坪山区', '深圳市坪山区比亚迪路3009号', '深圳市市场监督管理局',
 '存续', '10000人以上', 703500, '汽车(含小轿车)的研发、生产、销售及售后服务;锂离子电池、镍镉电池、镍氢电池、动力电池、储能电池及电池零配件的研发、生产、销售;塑胶制品、五金制品的研发、生产、销售;模具设计与制造;技术咨询和服务',
 '0755-89888888', 'ir@byd.com', 'https://www.byd.com',
 '中国500强,新能源汽车,高新技术', NULL,
 '002594', '比亚迪', '深A',
 1, NULL, NULL, 0, 0,
 NULL, NULL, NULL, NULL,
 'system', '2026-07-01 10:00:00', 0),

-- 4. 阿里巴巴(中国)网络技术有限公司
(4, 1,
 '阿里巴巴(中国)网络技术有限公司', '阿里', 'Alibaba (China) Network Technology Co., Ltd.', '91330100799655058B', '91330100799655058B',
 '电子商务', 'I', 'I64', 'I641', 'I6410',
 '戴珊', 1, '有限责任公司(外商投资)',
 '1590000.00万元', 'CNY', '1590000.00万元', 'CNY',
 '1999-09-09 00:00:00', '1999-09-09 00:00:00', '2024-02-20 00:00:00',
 '浙江省', '杭州市', '滨江区', '杭州市滨江区网商路699号', '浙江省市场监督管理局',
 '存续', '10000人以上', 124000, '增值电信业务;计算机软硬件、网络技术、电子商务技术开发、技术服务、技术咨询、技术转让;计算机软硬件的销售;设计、制作、代理、发布国内广告',
 '0571-85022088', 'ir@alibaba-inc.com', 'https://www.alibabagroup.com',
 '中国500强,电商龙头,高新技术', NULL,
 '09988', '阿里巴巴-W', '港股',
 1, NULL, NULL, 0, 0,
 NULL, NULL, NULL, NULL,
 'system', '2026-07-01 10:00:00', 0),

-- 5. 宁德时代新能源科技股份有限公司
(5, 1,
 '宁德时代新能源科技股份有限公司', '宁德时代', 'Contemporary Amperex Technology Co., Limited', '91350900MA32DHA7XR', '91350900MA32DHA7XR',
 '动力电池', 'C', 'C38', 'C384', 'C3840',
 '曾毓群', 1, '股份有限公司(上市)',
 '439880.9536万元', 'CNY', '439880.9536万元', 'CNY',
 '2011-12-16 00:00:00', '2011-12-16 00:00:00', '2024-06-12 00:00:00',
 '福建省', '宁德市', '蕉城区', '宁德市蕉城区漳湾镇新港路2号', '宁德市市场监督管理局',
 '存续', '10000人以上', 138000, '锂离子电池、锂聚合物电池、燃料电池、动力电池、超大容量储能电池及电池零配件的研发、生产、销售和服务;锂电池及相关产品的技术服务、测试服务以及咨询服务',
 '0593-2583666', 'catl@catlbattery.com', 'https://www.catl.com',
 '独角兽,动力电池龙头,高新技术', NULL,
 '300750', '宁德时代', '创业板',
 1, NULL, NULL, 0, 0,
 NULL, NULL, NULL, NULL,
 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 4. 企业地址 (sys_enterprise_address)
-- 列严格匹配 V55 schema
-- =============================================================================
INSERT INTO sys_enterprise_address (
    id, tenant_id, enterprise_id, base, city, district, reg_location,
    create_by, create_time, deleted
) VALUES
(1, 1, 1, '广东省', '深圳市', '龙岗区', '深圳市龙岗区坂田街道华为基地',     'system', '2026-07-01 10:00:00', 0),
(2, 1, 2, '广东省', '深圳市', '南山区', '深圳市南山区高新区科技中一路腾讯大厦', 'system', '2026-07-01 10:00:00', 0),
(3, 1, 3, '广东省', '深圳市', '坪山区', '深圳市坪山区比亚迪路3009号',         'system', '2026-07-01 10:00:00', 0),
(4, 1, 4, '浙江省', '杭州市', '滨江区', '杭州市滨江区网商路699号',              'system', '2026-07-01 10:00:00', 0),
(5, 1, 5, '福建省', '宁德市', '蕉城区', '宁德市蕉城区漳湾镇新港路2号',          'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 5. 企业介绍 (sys_enterprise_introduction)
-- 列严格匹配 V55 schema: business_scope, type, company_org_type, tags, percentile_score 等
-- 注意: V55 表没有 content/sort_order/status 列
-- =============================================================================
INSERT INTO sys_enterprise_introduction (
    id, tenant_id, enterprise_id,
    business_scope, type, company_org_type, tags, percentile_score,
    staff_num_range, social_staff_num,
    create_by, create_time, deleted
) VALUES
(1, 1, 1,
 'ICT基础设施、智能终端、云服务', 1, '有限责任公司(自然人投资或控股)', '世界500强,高新技术', 95,
 '10000人以上', 156000,
 'system', '2026-07-01 10:00:00', 0),
(2, 1, 2,
 '社交、游戏、金融科技、云', 1, '有限责任公司(外商投资)', '中国500强,互联网头部', 96,
 '10000人以上', 112000,
 'system', '2026-07-01 10:00:00', 0),
(3, 1, 3,
 '新能源汽车、动力电池、电子', 1, '股份有限公司(上市)', '中国500强,新能源汽车', 88,
 '10000人以上', 703500,
 'system', '2026-07-01 10:00:00', 0),
(4, 1, 4,
 '电商、云计算、数字媒体', 1, '有限责任公司(外商投资)', '中国500强,电商龙头', 92,
 '10000人以上', 124000,
 'system', '2026-07-01 10:00:00', 0),
(5, 1, 5,
 '动力电池、储能电池', 1, '股份有限公司(上市)', '独角兽,动力电池龙头', 90,
 '10000人以上', 138000,
 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 6. 工商登记 (sys_enterprise_register)
-- 列严格匹配 V55 schema: reg_number, reg_capital_currency, actual_capital, actual_capital_currency,
-- reg_institute, approved_time
-- 注意: V55 表没有 reg_no, reg_type, reg_capital, paid_capital, reg_address, reg_date,
-- reg_status, business_term, legal_rep, sort_order, status 列
-- =============================================================================
INSERT INTO sys_enterprise_register (
    id, tenant_id, enterprise_id,
    reg_number, reg_capital_currency, actual_capital, actual_capital_currency,
    reg_institute, approved_time,
    create_by, create_time, deleted
) VALUES
(1, 1, 1, '4403011029903889', 'CNY', '4044093.8266万元', 'CNY', '深圳市市场监督管理局', '2024-04-10 00:00:00', 'system', '2026-07-01 10:00:00', 0),
(2, 1, 2, '440301501124589',  'CNY', '2000000.00万元',    'CNY', '深圳市市场监督管理局', '2024-03-15 00:00:00', 'system', '2026-07-01 10:00:00', 0),
(3, 1, 3, '440301102920209',  'CNY', '291114.2855万元',   'CNY', '深圳市市场监督管理局', '2024-05-08 00:00:00', 'system', '2026-07-01 10:00:00', 0),
(4, 1, 4, '330100000015384',  'CNY', '1590000.00万元',    'CNY', '浙江省市场监督管理局', '2024-02-20 00:00:00', 'system', '2026-07-01 10:00:00', 0),
(5, 1, 5, '350900100006128',  'CNY', '439880.9536万元',   'CNY', '宁德市市场监督管理局', '2024-06-12 00:00:00', 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 7. 企业标签 (sys_enterprise_tag) - 列匹配 V53
-- =============================================================================
INSERT INTO sys_enterprise_tag (id, tenant_id, enterprise_id, tag_name, tag_color, sort_order, remark, create_by, create_time, deleted) VALUES
(1,  1, 1, '世界500强',     '#E74C3C', 1, NULL, 'system', '2026-07-01 10:00:00', 0),
(2,  1, 1, '高新技术',     '#3498DB', 2, NULL, 'system', '2026-07-01 10:00:00', 0),
(3,  1, 2, '中国500强',     '#E74C3C', 1, NULL, 'system', '2026-07-01 10:00:00', 0),
(4,  1, 2, '互联网头部',    '#2ECC71', 2, NULL, 'system', '2026-07-01 10:00:00', 0),
(5,  1, 3, '中国500强',     '#E74C3C', 1, NULL, 'system', '2026-07-01 10:00:00', 0),
(6,  1, 3, '新能源汽车',    '#F39C12', 2, NULL, 'system', '2026-07-01 10:00:00', 0),
(7,  1, 4, '中国500强',     '#E74C3C', 1, NULL, 'system', '2026-07-01 10:00:00', 0),
(8,  1, 4, '电商龙头',     '#9B59B6', 2, NULL, 'system', '2026-07-01 10:00:00', 0),
(9,  1, 5, '独角兽',       '#1ABC9C', 1, NULL, 'system', '2026-07-01 10:00:00', 0),
(10, 1, 5, '动力电池龙头',   '#E67E22', 2, NULL, 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 8. 行业类型 (sys_enterprise_industry) - 列匹配 V54
-- 注意: V54 无 enterprise_id 字段 (是行业字典表, 不关联企业)
-- =============================================================================
INSERT INTO sys_enterprise_industry (id, tenant_id, code, category, category_big, category_middle, category_small, status, create_by, create_time, deleted) VALUES
(1, 1, 'C3911', '制造业', '计算机、通信和其他电子设备制造业', '通信设备制造', '通信系统设备制造', 1, 'system', '2026-07-01 10:00:00', 0),
(2, 1, 'I6410', '信息传输、软件和信息技术服务业', '互联网和相关服务', '互联网接入及相关服务', '互联网接入及相关服务', 1, 'system', '2026-07-01 10:00:00', 0),
(3, 1, 'C3610', '制造业', '汽车制造业', '汽车整车制造', '汽车整车制造', 1, 'system', '2026-07-01 10:00:00', 0),
(4, 1, 'C3840', '制造业', '电气机械和器材制造业', '电池制造', '锂离子电池制造', 1, 'system', '2026-07-01 10:00:00', 0),
(5, 1, 'I6420', '信息传输、软件和信息技术服务业', '互联网和相关服务', '互联网安全服务', '互联网安全服务', 1, 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 9. 企业关注标签关联 (sys_enterprise_focus) - 列匹配 V57
-- =============================================================================
INSERT INTO sys_enterprise_focus (id, tenant_id, park_id, enterprise_id, focus_id, focus_name, focus_items, focus_item_names, status, create_by, create_time, deleted) VALUES
(1, 1, NULL, 1, 1, '企业规模',  '1',          '世界500强',                1, 'system', '2026-07-01 10:00:00', 0),
(2, 1, NULL, 2, 1, '企业规模',  '2',          '中国500强',                1, 'system', '2026-07-01 10:00:00', 0),
(3, 1, NULL, 3, 1, '企业规模',  '2',          '中国500强',                1, 'system', '2026-07-01 10:00:00', 0),
(4, 1, NULL, 4, 1, '企业规模',  '2',          '中国500强',                1, 'system', '2026-07-01 10:00:00', 0),
(5, 1, NULL, 5, 2, '企业类型',  '5',          '独角兽',                  1, 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 10. 客户信息 (sys_customer_information) - 列严格匹配 V58 schema
-- 注意: V58 表无 annual_revenue/total_assets/employee_count/tax_payment/
-- intellectual_property_income/requirement_* 字段
-- 改用 V58 实际字段: business_income, last_year_tax, total_investment_amount
-- =============================================================================
INSERT INTO sys_customer_information (
    id, tenant_id, park_id, enterprise_id, code, customer_type,
    area, settle_address, url,
    name, phone, manager_phone, email,
    regist_time, regist_money, rental_standard,
    industrial_field, main_business,
    company_strengths,
    valid_intellectual_property, invention_patents, utility_model_patent, industrial_design_patents,
    trademark, software_copyright,
    business_income, last_year_tax, total_investment_amount, total_financing_amount,
    suggest,
    structural_load, floor_height, capacitance,
    status, create_by, create_time, deleted
) VALUES
(1, 1, 1, 1, 'CUST-HW-001',   3, 50000.00, '深圳龙岗坂田基地A栋', NULL,
 '任正非', '400-822-9999', '13800000001', 'support@huawei.com',
 '1987-09-15', 40440938266.00, '120元/m²/月',
 5, 'ICT基础设施、智能终端、云服务',
 '1,2,9',  -- 世界500强, 中国500强, 高新技术企业
 95000, 35000, 25000, 8000, 12000, 18000,
 4, 6, 800000000.00, 100000000.00,
 '建议园区配套研发实验室',
 2, 2, 3,
 1, 'system', '2026-07-01 10:00:00', 0),
(2, 1, 1, 2, 'CUST-TX-001',   3, 35000.00, '深圳南山腾讯大厦', NULL,
 '马化腾', '0755-86013388', '13800000002', 'service@tencent.com',
 '1998-11-11', 20000000000.00, '150元/m²/月',
 5, '社交、游戏、金融科技、云',
 '1,2,9',
 65000, 28000, 18000, 5500, 9500, 12000,
 4, 6, 500000000.00, 80000000.00,
 '建议园区配套数据中心',
 2, 2, 3,
 1, 'system', '2026-07-01 10:00:00', 0),
(3, 1, 1, 3, 'CUST-BYD-001',  3, 80000.00, '深圳坪山比亚迪总部', NULL,
 '王传福', '0755-89888888', '13800000003', 'ir@byd.com',
 '1995-02-10', 2911142855.00, '90元/m²/月',
 4, '新能源汽车、动力电池、电子',
 '1,2',
 42000, 15000, 12000, 4000, 8000, 5000,
 4, 5, 300000000.00, 50000000.00,
 '建议园区配套电池研发实验室',
 3, 2, 3,
 1, 'system', '2026-07-01 10:00:00', 0),
(4, 1, 1, 4, 'CUST-ALB-001',  3, 60000.00, '杭州滨江阿里中心', NULL,
 '戴珊', '0571-85022088', '13800000004', 'ir@alibaba-inc.com',
 '1999-09-09', 15900000000.00, '140元/m²/月',
 5, '电商、云计算、数字媒体',
 '1,2,9',
 38000, 12000, 9000, 3000, 7000, 7000,
 4, 6, 700000000.00, 90000000.00,
 '建议园区配套大型数据中心',
 2, 2, 3,
 1, 'system', '2026-07-01 10:00:00', 0),
(5, 1, 1, 5, 'CUST-CATL-001', 3, 40000.00, '宁德蕉城基地', NULL,
 '曾毓群', '0593-2583666', '13800000005', 'catl@catlbattery.com',
 '2011-12-16', 4398809536.00, '100元/m²/月',
 4, '动力电池、储能电池',
 '1,5,9',  -- 中国500强, 独角兽, 高新技术
 18500, 8500, 5000, 1500, 2500, 1000,
 4, 5, 250000000.00, 30000000.00,
 '建议园区配套电池中试线',
 3, 2, 3,
 1, 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 11. 云企库数据 - 经营风险 (sys_enterprise_cloud_data, business_risk)
-- 列匹配 V63 schema
-- =============================================================================
INSERT INTO sys_enterprise_cloud_data (id, tenant_id, enterprise_id, enterprise_name, category, data_content, data_year, data_date, sort_order, status, remark, create_by, create_time, deleted) VALUES
(1, 1, 1, '华为技术有限公司', 'business_risk', '{"event":"无重大经营风险","score":"AAA","type":"综合评价"}', '2024', '2024-12-31', 1, 1, '2024 年度风险评估', 'system', '2026-07-01 10:00:00', 0),
(2, 1, 2, '腾讯科技(深圳)有限公司', 'business_risk', '{"event":"无重大经营风险","score":"AAA","type":"综合评价"}', '2024', '2024-12-31', 1, 1, '2024 年度风险评估', 'system', '2026-07-01 10:00:00', 0),
(3, 1, 3, '比亚迪股份有限公司', 'business_risk', '{"event":"1次环保处罚(2023年6月,已整改)","score":"AA","type":"行政处罚"}', '2024', '2024-12-31', 1, 1, '历史环保处罚已整改完毕', 'system', '2026-07-01 10:00:00', 0),
(4, 1, 4, '阿里巴巴(中国)网络技术有限公司', 'business_risk', '{"event":"1次反垄断处罚(2021年,182亿元,已缴纳)","score":"A","type":"行政处罚"}', '2024', '2024-12-31', 1, 1, '历史反垄断处罚已执行', 'system', '2026-07-01 10:00:00', 0),
(5, 1, 5, '宁德时代新能源科技股份有限公司', 'business_risk', '{"event":"无重大经营风险","score":"AAA","type":"综合评价"}', '2024', '2024-12-31', 1, 1, '2024 年度风险评估', 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 12. 云企库数据 - 司法风险 (sys_enterprise_cloud_data, judicial_risk)
-- =============================================================================
INSERT INTO sys_enterprise_cloud_data (id, tenant_id, enterprise_id, enterprise_name, category, data_content, data_year, data_date, sort_order, status, remark, create_by, create_time, deleted) VALUES
(6,  1, 1, '华为技术有限公司',                  'judicial_risk', '{"event":"0条","lawsuit":0,"execution":0,"dishonest":0}',  '2024', '2024-12-31', 1, 1, '2024 司法风险汇总',                          'system', '2026-07-01 10:00:00', 0),
(7,  1, 2, '腾讯科技(深圳)有限公司',            'judicial_risk', '{"event":"12条开庭公告","lawsuit":12,"execution":0,"dishonest":0}', '2024', '2024-12-31', 1, 1, '2024 司法风险汇总(均为游戏业务相关诉讼)',      'system', '2026-07-01 10:00:00', 0),
(8,  1, 3, '比亚迪股份有限公司',               'judicial_risk', '{"event":"5条开庭公告","lawsuit":5,"execution":0,"dishonest":0}',   '2024', '2024-12-31', 1, 1, '2024 司法风险汇总(部分专利诉讼)',            'system', '2026-07-01 10:00:00', 0),
(9,  1, 4, '阿里巴巴(中国)网络技术有限公司',     'judicial_risk', '{"event":"0条","lawsuit":0,"execution":0,"dishonest":0}',  '2024', '2024-12-31', 1, 1, '2024 司法风险汇总',                          'system', '2026-07-01 10:00:00', 0),
(10, 1, 5, '宁德时代新能源科技股份有限公司',     'judicial_risk', '{"event":"3条开庭公告","lawsuit":3,"execution":0,"dishonest":0}',   '2024', '2024-12-31', 1, 1, '2024 司法风险汇总(部分专利诉讼)',            'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 13. 云企库数据 - 经营状况 (sys_enterprise_cloud_data, business_situation)
-- =============================================================================
INSERT INTO sys_enterprise_cloud_data (id, tenant_id, enterprise_id, enterprise_name, category, data_content, data_year, data_date, sort_order, status, remark, create_by, create_time, deleted) VALUES
(11, 1, 1, '华为技术有限公司',                'business_situation', '{"taxCredit":"A","creditRating":"AAA","recruitCount":3200,"bidCount":158}',  '2024', '2024-12-31', 1, 1, '2024 经营状况', 'system', '2026-07-01 10:00:00', 0),
(12, 1, 2, '腾讯科技(深圳)有限公司',         'business_situation', '{"taxCredit":"A","creditRating":"AAA","recruitCount":8500,"bidCount":42}',   '2024', '2024-12-31', 1, 1, '2024 经营状况', 'system', '2026-07-01 10:00:00', 0),
(13, 1, 3, '比亚迪股份有限公司',             'business_situation', '{"taxCredit":"A","creditRating":"AA","recruitCount":28000,"bidCount":356}', '2024', '2024-12-31', 1, 1, '2024 经营状况', 'system', '2026-07-01 10:00:00', 0),
(14, 1, 4, '阿里巴巴(中国)网络技术有限公司', 'business_situation', '{"taxCredit":"A","creditRating":"AA","recruitCount":15000,"bidCount":28}',  '2024', '2024-12-31', 1, 1, '2024 经营状况', 'system', '2026-07-01 10:00:00', 0),
(15, 1, 5, '宁德时代新能源科技股份有限公司', 'business_situation', '{"taxCredit":"A","creditRating":"AAA","recruitCount":18000,"bidCount":124}', '2024', '2024-12-31', 1, 1, '2024 经营状况', 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 14. 云企库数据 - 企业详情 (sys_enterprise_cloud_data, ent_detail)
-- =============================================================================
INSERT INTO sys_enterprise_cloud_data (id, tenant_id, enterprise_id, enterprise_name, category, data_content, data_year, data_date, sort_order, status, remark, create_by, create_time, deleted) VALUES
(16, 1, 1, '华为技术有限公司',                'ent_detail', '{"patentCount":35000,"trademarkCount":12000,"softwareCopyright":18000,"websiteFiling":256}', '2024', '2024-12-31', 1, 1, '知识产权概况', 'system', '2026-07-01 10:00:00', 0),
(17, 1, 2, '腾讯科技(深圳)有限公司',         'ent_detail', '{"patentCount":28000,"trademarkCount":9500,"softwareCopyright":12000,"websiteFiling":312}',  '2024', '2024-12-31', 1, 1, '知识产权概况', 'system', '2026-07-01 10:00:00', 0),
(18, 1, 3, '比亚迪股份有限公司',             'ent_detail', '{"patentCount":15000,"trademarkCount":8000,"softwareCopyright":5000,"websiteFiling":128}',   '2024', '2024-12-31', 1, 1, '知识产权概况', 'system', '2026-07-01 10:00:00', 0),
(19, 1, 4, '阿里巴巴(中国)网络技术有限公司', 'ent_detail', '{"patentCount":12000,"trademarkCount":7000,"softwareCopyright":7000,"websiteFiling":425}',   '2024', '2024-12-31', 1, 1, '知识产权概况', 'system', '2026-07-01 10:00:00', 0),
(20, 1, 5, '宁德时代新能源科技股份有限公司', 'ent_detail', '{"patentCount":8500,"trademarkCount":2500,"softwareCopyright":1000,"websiteFiling":52}',     '2024', '2024-12-31', 1, 1, '知识产权概况', 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 15. 云企库数据 - 知识 (sys_enterprise_cloud_data, knowledge)
-- =============================================================================
INSERT INTO sys_enterprise_cloud_data (id, tenant_id, enterprise_id, enterprise_name, category, data_content, data_year, data_date, sort_order, status, remark, create_by, create_time, deleted) VALUES
(21, 1, 1, '华为技术有限公司',                'knowledge', '{"title":"华为发布2024年年度报告","summary":"营收7042亿元,同比增长9.6%","source":"华为官网"}', '2024', '2024-12-31', 1, 1, '年度报告', 'system', '2026-07-01 10:00:00', 0),
(22, 1, 2, '腾讯科技(深圳)有限公司',         'knowledge', '{"title":"腾讯2024年报","summary":"营收6098亿元,同比增长8%","source":"腾讯官网"}',         '2024', '2024-12-31', 1, 1, '年度报告', 'system', '2026-07-01 10:00:00', 0),
(23, 1, 3, '比亚迪股份有限公司',             'knowledge', '{"title":"比亚迪2024年销量","summary":"新能源汽车销量427万辆,蝉联全球销冠","source":"比亚迪官网"}', '2024', '2024-12-31', 1, 1, '年度报告', 'system', '2026-07-01 10:00:00', 0),
(24, 1, 4, '阿里巴巴(中国)网络技术有限公司', 'knowledge', '{"title":"阿里2024财年报告","summary":"营收9411亿元,AI业务高速增长","source":"阿里官网"}',         '2024', '2024-12-31', 1, 1, '年度报告', 'system', '2026-07-01 10:00:00', 0),
(25, 1, 5, '宁德时代新能源科技股份有限公司', 'knowledge', '{"title":"宁德时代2024年业绩","summary":"营收4009亿元,动力电池市占率全球第一","source":"CATL官网"}',    '2024', '2024-12-31', 1, 1, '年度报告', 'system', '2026-07-01 10:00:00', 0);


-- =============================================================================
-- 16. 企业概览数据 (sys_enterprise_overview_data)
-- 列匹配 V63 schema
-- =============================================================================
INSERT INTO sys_enterprise_overview_data (
    id, tenant_id, enterprise_id, enterprise_name,
    reg_capital, total_assets, annual_revenue,
    employee_count, patent_count, trademark_count, copyright_count,
    risk_count, bid_count,
    equity_structure_json, overview_json,
    sort_order, status, remark,
    create_by, create_time, deleted
) VALUES
(1, 1, 1, '华为技术有限公司',
 '4044093.83万元', '10258亿元', '7042亿元',
 207000, 35000, 12000, 18000,
 0, 158,
 '[{"shareholder":"任正非","ratio":"0.65%"},{"shareholder":"工会委员会","ratio":"99.35%"}]',
 '{"industry":"通信设备","world500":true,"listing":"未上市","highTech":true,"founded":"1987-09-15"}',
 1, 1, '世界500强,非上市',
 'system', '2026-07-01 10:00:00', 0),
(2, 1, 2, '腾讯科技(深圳)有限公司',
 '2000000.00万元', '9680亿元', '6098亿元',
 112000, 28000, 9500, 12000,
 12, 42,
 '[{"shareholder":"马化腾","ratio":"8.41%"},{"shareholder":"Naspers/Prosus","ratio":"25.82%"},{"shareholder":"公众流通股","ratio":"65.77%"}]',
 '{"industry":"互联网","world500":true,"listing":"港股00700","highTech":true,"founded":"1998-11-11"}',
 2, 1, '世界500强,港股上市',
 'system', '2026-07-01 10:00:00', 0),
(3, 1, 3, '比亚迪股份有限公司',
 '291114.29万元', '7632亿元', '6023亿元',
 703500, 15000, 8000, 5000,
 5, 356,
 '[{"shareholder":"王传福","ratio":"17.64%"},{"shareholder":"吕向阳","ratio":"8.21%"},{"shareholder":"公众流通股","ratio":"74.15%"}]',
 '{"industry":"新能源汽车","world500":true,"listing":"A+H 002594/1211","highTech":true,"founded":"1995-02-10"}',
 3, 1, '世界500强,A+H上市',
 'system', '2026-07-01 10:00:00', 0),
(4, 1, 4, '阿里巴巴(中国)网络技术有限公司',
 '1590000.00万元', '13020亿元', '9411亿元',
 219260, 12000, 7000, 7000,
 1, 28,
 '[{"shareholder":"软银集团","ratio":"13.97%"},{"shareholder":"Altaba","ratio":"11.32%"},{"shareholder":"马云及合伙人","ratio":"6.20%"},{"shareholder":"公众流通股","ratio":"68.51%"}]',
 '{"industry":"电子商务","world500":true,"listing":"港股09988/美股BABA","highTech":true,"founded":"1999-09-09"}',
 4, 1, '世界500强,港股+美股上市',
 'system', '2026-07-01 10:00:00', 0),
(5, 1, 5, '宁德时代新能源科技股份有限公司',
 '439880.95万元', '6276亿元', '4009亿元',
 138000, 8500, 2500, 1000,
 3, 124,
 '[{"shareholder":"曾毓群","ratio":"23.27%"},{"shareholder":"黄世霖","ratio":"10.59%"},{"shareholder":"公众流通股","ratio":"66.14%"}]',
 '{"industry":"动力电池","world500":true,"listing":"创业板300750","highTech":true,"founded":"2011-12-16"}',
 5, 1, '世界500强,创业板上市',
 'system', '2026-07-01 10:00:00', 0);
