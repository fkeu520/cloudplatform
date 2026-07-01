package com.cloudhub.platform.enterprise.cloud.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 云企库数据分类枚举 (Phase 3)
 *
 * <p>对应 csyh 11 个模块: overview/business_risk/business_situation/ent_detail/
 * enterprise_image/enterprise_recruit/judicial_risk/knowledge/enterprise_search.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Getter
@RequiredArgsConstructor
public enum CloudDataCategoryEnum {

    /** 经营风险 — 税务违法/异常/行政处罚/动产抵押/股权质押/知识产权质押/清算/注销 */
    BUSINESS_RISK("business_risk", "经营风险"),

    /** 经营状况 — 税务评级/信用评级/招投标/新闻舆论/企业招聘 */
    BUSINESS_SITUATION("business_situation", "经营状况"),

    /** 企业详情 — 专利/软著/作品著作权/网站备案/商标 */
    ENT_DETAIL("ent_detail", "企业详情"),

    /** 司法风险 — 开庭公告/法院公告/法律诉讼/失信/被执行人/破产等 */
    JUDICIAL_RISK("judicial_risk", "司法风险"),

    /** 知识 — 新闻、企业相关信息 */
    KNOWLEDGE("knowledge", "企业知识"),

    ;

    private final String code;
    private final String name;
}
