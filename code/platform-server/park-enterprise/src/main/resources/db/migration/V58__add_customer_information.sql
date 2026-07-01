-- =============================================================================
-- V58__add_customer_information.sql
-- park-enterprise Phase 2 提前 - 客户信息
-- 来源: csyh pai-enterprise-csyh-2.x V2022031401 ddl + 50+ 业务字段
-- 客户拓展调查表 (入驻意向/业务/知识产权/财务/需求/物理)
-- =============================================================================

CREATE TABLE sys_customer_information (
    id                                  BIGINT         NOT NULL                       COMMENT '雪花 ID',
    tenant_id                           BIGINT         DEFAULT NULL                   COMMENT '租户 ID',
    park_id                             BIGINT         DEFAULT NULL                   COMMENT '园区 ID',

    -- 基本信息
    enterprise_id                       BIGINT         DEFAULT NULL                   COMMENT '关联企业 ID (sys_enterprise.id, 可空=未关联)',
    code                                VARCHAR(64)    DEFAULT NULL                   COMMENT '项目编号',
    customer_type                       INT            DEFAULT NULL                   COMMENT '客户类型 1=潜在 2=意向 3=已签约',

    -- 入驻信息
    area                                DECIMAL(16, 2) DEFAULT NULL                   COMMENT '租用面积 (m²)',
    settle_address                      VARCHAR(500)   DEFAULT NULL                   COMMENT '入驻地址',
    url                                 VARCHAR(500)   DEFAULT NULL                   COMMENT '生成链接 (客户专属填报链接)',

    -- 负责人
    name                                VARCHAR(64)    DEFAULT NULL                   COMMENT '负责人姓名',
    phone                               VARCHAR(32)    DEFAULT NULL                   COMMENT '电话',
    manager_phone                       VARCHAR(32)    DEFAULT NULL                   COMMENT '客户手机号 (短信验证)',
    email                               VARCHAR(128)   DEFAULT NULL                   COMMENT '邮箱',

    -- 注册信息
    regist_time                         DATETIME       DEFAULT NULL                   COMMENT '注册时间',
    regist_money                        DECIMAL(16, 2) DEFAULT NULL                   COMMENT '注册资本 (元)',
    rental_standard                     VARCHAR(64)    DEFAULT NULL                   COMMENT '租金标准 (元/m²/月)',

    -- 产业领域 (1-6)
    industrial_field                    INT            DEFAULT NULL                   COMMENT '所属产业领域 1=集成电路 2=生物医药 3=新材料 4=新能源 5=智能制造 6=信创',
    industrial_field_other              VARCHAR(64)    DEFAULT NULL                   COMMENT '其他产业领域',
    main_business                       VARCHAR(500)   DEFAULT NULL                   COMMENT '主营业务',

    -- 企业实力 (1-10 多选用逗号)
    company_strengths                   VARCHAR(64)    DEFAULT NULL                   COMMENT '企业实力 1=上市公司 2=规上 3=独角兽 4=瞪羚 5=专精特新 6=高企培育 7=科技型中小 8=外资 9=市高级人才 10=其他',
    company_strengths_other             VARCHAR(64)    DEFAULT NULL                   COMMENT '其他企业实力',

    -- 知识产权 (8 项)
    valid_intellectual_property         INT            DEFAULT NULL                   COMMENT '有效知识产权总数量',
    invention_patents                   INT            DEFAULT NULL                   COMMENT '发明专利数量',
    utility_model_patent                INT            DEFAULT NULL                   COMMENT '实用新型专利数量',
    industrial_design_patents           INT            DEFAULT NULL                   COMMENT '外观设计专利数量',
    trademark                           INT            DEFAULT NULL                   COMMENT '商标数量',
    software_copyright                  INT            DEFAULT NULL                   COMMENT '软件著作权',
    new_plant_variety                   INT            DEFAULT NULL                   COMMENT '植物新品种',
    integrated_circuit_layout          INT            DEFAULT NULL                   COMMENT '集成电路布图',
    purchase_foreign_patents            INT            DEFAULT NULL                   COMMENT '购买国外专利',
    other_patents                       INT            DEFAULT NULL                   COMMENT '其他专利',

    -- 财务状况
    business_income                     INT            DEFAULT NULL                   COMMENT '营业收入 1=500万以下 2=500-2000万 3=2000万-1亿 4=1亿以上',
    last_year_tax                       INT            DEFAULT NULL                   COMMENT '上年度税收 1=10万以下 2=10-50万 3=50-100万 4=100-300万 5=300-500万 6=500万以上',
    total_investment_amount             DECIMAL(16, 2) DEFAULT NULL                   COMMENT '投资总金额 (元)',
    total_financing_amount              DECIMAL(16, 2) DEFAULT NULL                   COMMENT '融资总金额 (元)',

    -- 困难 + 需求
    company_difficulties                VARCHAR(64)    DEFAULT NULL                   COMMENT '面临困难 1=市场需求不足 2=资金紧张 3=融资渠道狭窄 4=人才短缺 5=其他',
    company_difficulties_other          VARCHAR(64)    DEFAULT NULL                   COMMENT '其他困难',
    supporting_services                 VARCHAR(64)    DEFAULT NULL                   COMMENT '配套服务需求 1=班车 2=儿童托管 3=网络通讯 4=团餐 5=公寓 6=室内环境',
    technical_consulting_services       VARCHAR(128)   DEFAULT NULL                   COMMENT '科技咨询服务 1=项目申报 2=高企认定 3=成果鉴定 4=知识产权贯标 5=创新平台认定 6=政策推送解读',
    management_services                 VARCHAR(64)    DEFAULT NULL                   COMMENT '管理服务 1=工商注册 2=法律服务 3=人力资源 4=团建 5=培训 6=其他',
    management_services_other           VARCHAR(64)    DEFAULT NULL                   COMMENT '其他管理服务',
    technology_platform_services        VARCHAR(64)    DEFAULT NULL                   COMMENT '公共技术平台 1=基础研发 2=中试 3=分析检测 4=成果转化 5=技术合作 6=仪器共享 7=其他',
    technology_platform_services_other  VARCHAR(64)    DEFAULT NULL                   COMMENT '其他公共技术平台',
    investment_services                 VARCHAR(64)    DEFAULT NULL                   COMMENT '投资服务 1=基金 2=VC/PE 3=金融贷款 4=融资租赁 5=租金换股权 6=产权换股权 7=其他',
    investment_services_other           VARCHAR(64)    DEFAULT NULL                   COMMENT '其他投资服务',
    suggest                             TEXT                                          COMMENT '对产业园建议',

    -- 物理需求
    structural_load                     INT            DEFAULT NULL                   COMMENT '结构荷载 1=2kN/m²以下 2=2-3kN/m² 3=3-5kN/m² 4=5kN/m²以上',
    floor_height                        INT            DEFAULT NULL                   COMMENT '楼层高度 1=3.8m以下 2=3.8-4.5m 3=4.5m以上',
    capacitance                         INT            DEFAULT NULL                   COMMENT '电容量 1=100KW及以下 2=500KW及以下 3=1000KW及以下',
    supply_and_drainage                  VARCHAR(32)    DEFAULT NULL                   COMMENT '1=需要给水 2=需要排水',
    fresh_air_smoke_exhaust             INT            DEFAULT NULL                   COMMENT '新风排烟 1=需要 2=不需要',
    elevator_length                     INT            DEFAULT NULL                   COMMENT '电梯长度 (mm)',
    elevator_width                      INT            DEFAULT NULL                   COMMENT '电梯宽度 (mm)',
    elevator_height                     INT            DEFAULT NULL                   COMMENT '电梯高度 (mm)',
    elevator_load                       INT            DEFAULT NULL                   COMMENT '电梯荷载 1=1000kg 2=1350kg 3=1600kg 4=其他',

    -- 自身状态
    status                              INT            NOT NULL DEFAULT 1              COMMENT '启用状态 (1=有效 0=失效)',

    -- BaseEntity
    create_by                           VARCHAR(64)    DEFAULT NULL                   COMMENT '创建人',
    create_time                         DATETIME       DEFAULT NULL                   COMMENT '创建时间',
    update_by                           VARCHAR(64)    DEFAULT NULL                   COMMENT '更新人',
    update_time                         DATETIME       DEFAULT NULL                   COMMENT '更新时间',
    deleted                             INT            NOT NULL DEFAULT 0              COMMENT '逻辑删除',

    PRIMARY KEY (id),
    KEY idx_cust_tenant (tenant_id),
    KEY idx_cust_park (park_id),
    KEY idx_cust_enterprise (enterprise_id),
    KEY idx_cust_type (customer_type),
    KEY idx_cust_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户信息 (park-enterprise V58)';
