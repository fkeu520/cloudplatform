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

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSender implements ChannelSender {

    private final MessageChannelService messageChannelService;

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
            Config aliyunConfig = new Config()
                    .setAccessKeyId(config.getAccessKeyId())
                    .setAccessKeySecret(config.getAccessKeySecret());
            aliyunConfig.setEndpoint("dysmsapi.aliyuncs.com");
            Client client = new Client(aliyunConfig);

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
