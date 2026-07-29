package com.cloudhub.platform.workflow.init;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;

/**
 * 流程定义自动初始化 Runner
 * <p>
 * 启动时扫描 classpath:init-bpmn/*.bpmn, 对每个文件:
 * <ol>
 *   <li>解析 BPMN XML 拿到 {@code <bpmn:process id="...">}</li>
 *   <li>查询 ACT_RE_PROCDEF 是否已有同 key 的流程定义</li>
 *   <li>没有则自动部署 (Insert 到 ACT_RE_DEPLOYMENT + ACT_GE_BYTEARRAY + ACT_RE_PROCDEF)</li>
 * </ol>
 * <p>
 * 设计原则:
 * <ul>
 *   <li>幂等: 同一个 BPMN 多次启动只部署一次</li>
 *   <li>失败不阻塞启动: 单个 BPMN 部署失败只记 warn, 应用继续</li>
 *   <li>key 存在即跳过: 业务方 (Leave.vue) 已部署的流程不会被覆盖 (确保新功能自动覆盖)</li>
 * </ul>
 * <p>
 * 业务场景:
 * <ul>
 *   <li>全新部署 142: 没有 ACT_RE_PROCDEF, 自动部署 leave-approval 流程</li>
 *   <li>已有手动部署: key 已存在, 跳过, 不污染</li>
 *   <li>业务方设计器创建新版流程: VERSION + 1, 旧 VERSION 保留, 不冲突</li>
 * </ul>
 */
@Slf4j
@Component
public class InitBpmnRunner implements ApplicationRunner {

    private final RepositoryService repositoryService;

    @Value("classpath:init-bpmn/*.bpmn")
    private Resource[] initBpmnResources;

    public InitBpmnRunner(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (initBpmnResources == null || initBpmnResources.length == 0) {
            log.info("[InitBpmn] classpath:init-bpmn/ 下无 BPMN 文件, 跳过自动初始化");
            return;
        }
        log.info("[InitBpmn] 扫描到 {} 个初始 BPMN 文件, 开始检查 + 自动部署", initBpmnResources.length);
        int deployed = 0;
        int skipped = 0;
        int failed = 0;
        for (Resource res : initBpmnResources) {
            String filename = res.getFilename();
            try {
                String processKey = extractProcessKey(res);
                if (processKey == null || processKey.isBlank()) {
                    log.warn("[InitBpmn] {} 缺少 <bpmn:process id=\"...\">, 跳过", filename);
                    failed++;
                    continue;
                }
                if (!isValidNCName(processKey)) {
                    log.warn("[InitBpmn] {} processKey='{}' 不是合法 NCName (XML id 规则), 跳过", filename, processKey);
                    failed++;
                    continue;
                }
                // 幂等检查: key 存在即跳过 (无论 VERSION)
                long existing = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(processKey)
                        .count();
                if (existing > 0) {
                    log.info("[InitBpmn] {} key='{}' 已存在 (count={}), 跳过", filename, processKey, existing);
                    skipped++;
                    continue;
                }
                // 自动部署
                try (InputStream is = res.getInputStream()) {
                    Deployment deployment = repositoryService.createDeployment()
                            .name("auto-init: " + filename)
                            .key("auto-init-" + processKey + "-" + System.currentTimeMillis())
                            .addInputStream(filename, is)
                            .deploy();
                    log.info("[InitBpmn] {} 部署成功: deploymentId={}, key={}",
                            filename, deployment.getId(), processKey);
                    deployed++;
                }
            } catch (Exception e) {
                // 单个 BPMN 失败不阻塞其他, 也不阻塞应用启动
                log.warn("[InitBpmn] {} 部署失败, 跳过 (应用继续): {} | cause={}",
                        filename, e.getMessage(), e.getClass().getName());
                failed++;
            }
        }
        log.info("[InitBpmn] 初始化完成: deployed={}, skipped={}, failed={}",
                deployed, skipped, failed);
    }

    /**
     * 解析 BPMN XML, 提取 {@code <bpmn:process id="...">} 的 id
     * <p>
     * 用 DOM parser 避免引入新依赖 (JDK 自带 javax.xml.parsers).
     * 移除 XML 注释避免注释里的 process id 被误读.
     */
    private String extractProcessKey(Resource res) throws Exception {
        try (InputStream is = res.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 关键: 启用 namespace-aware, 否则 getElementsByTagNameNS 找不到 bpmn:process
            factory.setNamespaceAware(true);
            // 防止 XXE 攻击 (Flowable 推荐的硬编码安全配置)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(is));
            NodeList processes = doc.getElementsByTagNameNS(
                    "http://www.omg.org/spec/BPMN/20100524/MODEL", "process");
            if (processes.getLength() == 0) {
                // Fallback: 部分 BPMN 工具不带 namespace
                processes = doc.getElementsByTagName("process");
            }
            if (processes.getLength() > 0) {
                Element process = (Element) processes.item(0);
                return process.getAttribute("id");
            }
            return null;
        }
    }

    /**
     * NCName 校验 (XML Name 规则, 同 WorkflowDefinitionService 保持一致):
     * 首字符 [A-Za-z_], 后续 [A-Za-z0-9_.\-]
     */
    private boolean isValidNCName(String s) {
        if (s == null || s.isEmpty()) return false;
        char first = s.charAt(0);
        if (!isAsciiLetter(first) && first != '_') return false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isAsciiLetter(c) && !isAsciiDigit(c) && c != '_' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }
}