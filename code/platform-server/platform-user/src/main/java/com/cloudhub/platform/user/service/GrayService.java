package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.user.domain.entity.Config;
import com.cloudhub.platform.user.domain.entity.GrayAudit;
import com.cloudhub.platform.user.mapper.ConfigMapper;
import com.cloudhub.platform.user.mapper.GrayAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 灰度开关服务 (gray-release-infrastructure PR4)
 * <p>
 * 业务流:
 * <ol>
 *   <li>POST /gray/switch → toggle sys_config 中同名键 + 写 sys_gray_audit (同事务)</li>
 *   <li>GET /gray/audit/page → 查 sys_gray_audit 历史</li>
 *   <li>ConfigController.update → 若 configKey 以 "platform." 开头, 同步写 audit</li>
 * </ol>
 * <p>
 * 注意: Nacos 推送不在本服务范围, 由 PR2 @RefreshScope 自动 Bean 重建 + 运维
 * 手动 INSERT sys_gray_audit 记录 (后续可加 Nacos 长连接监听自动写).
 *
 * @author cloudhub
 * @since 2026-06-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrayService {

    private final ConfigMapper configMapper;
    private final GrayAuditMapper grayAuditMapper;

    /**
     * 修改灰度开关 (业务主入口)
     *
     * @param configKey 配置键 (必须以 platform. 开头)
     * @param newValue 新值
     * @param reason 变更原因 (业务方填写, 必填)
     * @param operatorId 操作人 ID (可空, 系统管理员场景)
     * @param operatorName 操作人姓名 (冗余)
     */
    @Transactional
    public void toggle(String configKey, String newValue, String reason,
                       Long operatorId, String operatorName) {
        if (configKey == null || !configKey.startsWith("platform.")) {
            throw new BizException("灰度开关 key 必须以 platform. 开头");
        }
        if (newValue == null) {
            throw new BizException("新值不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new BizException("变更原因必填 (审计要求)");
        }

        Config config = configMapper.selectByKey(configKey);
        String oldValue = config != null ? config.getConfigValue() : null;

        // upsert: 不存在则新增, 存在则更新
        if (config == null) {
            Config c = new Config();
            c.setConfigKey(configKey);
            c.setConfigName("灰度开关: " + configKey);
            c.setConfigValue(newValue);
            c.setConfigType(1); // 系统内置
            c.setRemark(reason);
            c.setTenantId(1L);
            configMapper.insert(c);
        } else {
            config.setConfigValue(newValue);
            if (reason != null) config.setRemark(reason);
            configMapper.updateById(config);
        }

        // 写审计
        GrayAudit audit = new GrayAudit();
        audit.setSwitchKey(configKey);
        audit.setGroupName(extractGroupName(configKey));
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setOpType("DB_UPDATE");
        audit.setOperatorId(operatorId);
        audit.setOperatorName(operatorName != null ? operatorName : "system");
        audit.setReason(reason);
        audit.setServiceName("platform-user");
        audit.setInstanceIp(getLocalIp());
        grayAuditMapper.insert(audit);

        log.info("GrayService: toggle switch_key={} old={} new={} operator={} reason={}",
                configKey, oldValue, newValue, operatorName, reason);
    }

    /**
     * 审计历史分页查询
     */
    public PageResult<GrayAudit> history(String switchKey, int pageNum, int pageSize) {
        LambdaQueryWrapper<GrayAudit> w = new LambdaQueryWrapper<>();
        if (switchKey != null && !switchKey.isBlank()) {
            w.eq(GrayAudit::getSwitchKey, switchKey);
        }
        w.orderByDesc(GrayAudit::getCreateTime);
        Page<GrayAudit> p = grayAuditMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /**
     * 写审计 (供 ConfigService.update 调用, 用于 platform.* 配置变更留痕)
     */
    public void writeAuditFromConfigUpdate(Config config, String oldValue,
                                            String operatorName, String reason) {
        GrayAudit audit = new GrayAudit();
        audit.setSwitchKey(config.getConfigKey());
        audit.setGroupName(extractGroupName(config.getConfigKey()));
        audit.setOldValue(oldValue);
        audit.setNewValue(config.getConfigValue());
        audit.setOpType("DB_UPDATE");
        audit.setOperatorName(operatorName != null ? operatorName : "system");
        audit.setReason(reason != null ? reason : "经 ConfigController.update 修改");
        audit.setServiceName("platform-user");
        audit.setInstanceIp(getLocalIp());
        grayAuditMapper.insert(audit);
    }

    /**
     * 从 key 提取分组 (M4 P0-1 / M5 P0-2 等)
     */
    private String extractGroupName(String key) {
        if (key.startsWith("platform.tenant.")) return "M4 P0-1 多租户拦截器";
        if (key.startsWith("platform.data-scope.")) return "M5 P0-2 数据权限升级";
        return "其他";
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}