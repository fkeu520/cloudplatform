package com.cloudhub.platform.message.channel.impl;

import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.cloudhub.platform.message.channel.ChannelSender;
import com.cloudhub.platform.message.config.SmsConfig;
import com.cloudhub.platform.message.domain.entity.MessageChannel;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.service.MessageChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信发送器 - 通过阿里云 SMS API 发送短信
 * <p>
 * <b>安全风险:</b> 本实现直接读取 {@code message_channel.config_json} 中的明文密钥
 * ({@code accessKeyId}/{@code accessKeySecret}), 配置以 JSON 明文存储在数据库中.
 * 生产环境应集成 KMS (密钥管理服务) 或使用阿里云 STS 临时凭证, 禁止明文存密钥.
 * </p>
 *
 * <h3>迁移方案 (KMS):</h3>
 * <ol>
 *   <li>在 message_channel 的 config_json 中增加字段 {@code kmsKeyId}
 *       (或 {@code secretName}) 指向 KMS 中的加密密钥</li>
 *   <li>修改 {@link #loadConfig()} 从 KMS 解密, 不再 parse config_json 中的明文 key</li>
 *   <li>或改用阿里云 STS: config_json 存储 roleArn, 运行时 AssumeRole 获取临时凭证</li>
 * </ol>
 *
 * @deprecated 当前实现存在安全风险, 明文密钥存储在数据库中.
 * 请在 config_json 中删除明文 accessKeySecret, 改用 KMS/STS.
 * 详情见 MessageChannel 配置文档.
 */
@Deprecated
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSender implements ChannelSender {

    private final MessageChannelService messageChannelService;

    // 缓存 Client 实例，配置变更时重建
    private volatile Client cachedClient;
    private volatile String cachedConfigJson;

    @Override
    public String channelCode() {
        return "sms";
    }

    @Override
    public void send(MessageRecord record) {
        SmsConfig config = loadConfig();
        if (config == null) {
            log.warn("[短信] 渠道配置不存在或无效，跳过发送");
            return;
        }
        try {
            Client client = getOrCreateClient(config);
            if (client == null) {
                throw new RuntimeException("短信客户端初始化失败");
            }

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(record.getReceiverAddress())
                    .setSignName(config.getSignName())
                    .setTemplateCode(record.getContent())
                    .setTemplateParam("{}");

            SendSmsResponse response = client.sendSms(request);
            if (response.getBody() != null && "OK".equals(response.getBody().getCode())) {
                log.info("[短信] 发送成功 phone={}, bizId={}",
                        record.getReceiverAddress(), response.getBody().getBizId());
            } else {
                String code = response.getBody() != null ? response.getBody().getCode() : "unknown";
                String msg = response.getBody() != null ? response.getBody().getMessage() : "无响应";
                log.warn("[短信] 发送失败 phone={}, code={}, message={}",
                        record.getReceiverAddress(), code, msg);
                throw new RuntimeException("短信发送失败: " + msg);
            }
        } catch (Exception e) {
            log.error("[短信] 发送异常 phone={}", record.getReceiverAddress(), e);
            throw new RuntimeException("短信发送异常: " + e.getMessage());
        }
    }

    private Client getOrCreateClient(SmsConfig config) {
        try {
            String configJson = JSON.toJSONString(config);
            if (cachedClient != null && configJson.equals(cachedConfigJson)) {
                return cachedClient;
            }
            synchronized (this) {
                if (cachedClient != null && configJson.equals(cachedConfigJson)) {
                    return cachedClient;
                }
                Config aliyunConfig = new Config()
                        .setAccessKeyId(config.getAccessKeyId())
                        .setAccessKeySecret(config.getAccessKeySecret());
                aliyunConfig.setEndpoint("dysmsapi.aliyuncs.com");
                cachedClient = new Client(aliyunConfig);
                cachedConfigJson = configJson;
                return cachedClient;
            }
        } catch (Exception e) {
            log.error("[短信] 创建客户端失败", e);
            return null;
        }
    }

    private SmsConfig loadConfig() {
        try {
            MessageChannel channel = messageChannelService.lambdaQuery()
                    .eq(MessageChannel::getChannelCode, "sms")
                    .one();
            if (channel == null || channel.getConfigJson() == null || channel.getConfigJson().isBlank()) {
                return null;
            }
            return JSON.parseObject(channel.getConfigJson(), SmsConfig.class);
        } catch (Exception e) {
            log.warn("[短信] 加载渠道配置失败", e);
            return null;
        }
    }
}
