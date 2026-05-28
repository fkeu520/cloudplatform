package com.cloudhub.platform.message.channel.impl;

import com.alibaba.fastjson2.JSON;
import com.cloudhub.platform.message.channel.ChannelSender;
import com.cloudhub.platform.message.domain.entity.MessageChannel;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.service.MessageChannelService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender implements ChannelSender {

    private final MessageChannelService messageChannelService;

    @Override
    public String channelCode() {
        return "email";
    }

    @Override
    public void send(MessageRecord record) {
        Map<String, String> config = loadConfig();
        if (config == null) {
            log.warn("[邮件] 渠道配置不存在或无效，跳过发送");
            return;
        }
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(config.getOrDefault("host", "smtp.example.com"));
            sender.setPort(Integer.parseInt(config.getOrDefault("port", "587")));
            sender.setUsername(config.get("username"));
            sender.setPassword(config.get("password"));
            sender.setDefaultEncoding("UTF-8");

            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(config.getOrDefault("from", config.get("username"))));
            helper.setTo(record.getReceiverAddress());
            helper.setSubject(record.getTitle());
            helper.setText(record.getContent(), true);

            sender.send(message);
            log.info("[邮件] 发送成功 to={}, subject={}", record.getReceiverAddress(), record.getTitle());
        } catch (Exception e) {
            log.error("[邮件] 发送异常 to={}", record.getReceiverAddress(), e);
            throw new RuntimeException("邮件发送异常: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadConfig() {
        try {
            MessageChannel channel = messageChannelService.lambdaQuery()
                    .eq(MessageChannel::getChannelCode, "email")
                    .one();
            if (channel == null || channel.getConfigJson() == null) return null;
            return JSON.parseObject(channel.getConfigJson(), Map.class);
        } catch (Exception e) {
            log.warn("[邮件] 加载渠道配置失败", e);
            return null;
        }
    }
}
