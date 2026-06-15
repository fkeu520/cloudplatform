package com.cloudhub.platform.workflow.init;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * InitBpmnRunner 单元测试
 * <p>
 * 核心测试点:
 * - BPMN key 不存在 → 部署
 * - BPMN key 已存在 → 跳过 (幂等)
 * - 非法 NCName / 缺 process id → 跳过, 不抛异常
 * - 单个 BPMN 部署失败 → 不影响其他
 * - 启动失败不能让应用启动失败 (异常被吞掉)
 * <p>
 * Mock 技巧: 不用 RETURNS_DEEP_STUBS, 手动 mock chain, 避免 deep stubs 跨测试串扰.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InitBpmnRunnerTest {

    @Mock
    private RepositoryService repositoryService;
    @Mock
    private ProcessDefinitionQuery processDefinitionQuery;
    @Mock
    private DeploymentBuilder deploymentBuilder;
    @Mock
    private Deployment deployment;

    private InitBpmnRunner runner;

    private static final String VALID_BPMN =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">\n" +
            "  <bpmn:process id=\"leave-approval\" name=\"请假审批\" isExecutable=\"true\">\n" +
            "    <bpmn:startEvent id=\"start\"/>\n" +
            "    <bpmn:userTask id=\"task1\"/>\n" +
            "    <bpmn:endEvent id=\"end\"/>\n" +
            "    <bpmn:sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"task1\"/>\n" +
            "    <bpmn:sequenceFlow id=\"f2\" sourceRef=\"task1\" targetRef=\"end\"/>\n" +
            "  </bpmn:process>\n" +
            "</bpmn:definitions>";

    @BeforeEach
    void setUp() {
        runner = new InitBpmnRunner(repositoryService);
        // 手动 mock chain: 每次重新设置, 避免 stub 串扰
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(anyString())).thenReturn(processDefinitionQuery);
        when(repositoryService.createDeployment()).thenReturn(deploymentBuilder);
        when(deploymentBuilder.name(anyString())).thenReturn(deploymentBuilder);
        when(deploymentBuilder.key(anyString())).thenReturn(deploymentBuilder);
        when(deploymentBuilder.addInputStream(anyString(), any())).thenReturn(deploymentBuilder);
        when(deploymentBuilder.deploy()).thenReturn(deployment);
    }

    private Resource makeBpmn(String xml, String name) {
        return new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8), name);
    }

    private void injectBpmns(Resource... resources) {
        ReflectionTestUtils.setField(runner, "initBpmnResources", resources);
    }

    @Test
    @DisplayName("TC-01: BPMN key 不存在 → 自动部署")
    void testDeployWhenKeyNotExists() {
        injectBpmns(makeBpmn(VALID_BPMN, "leave-approval.bpmn"));
        when(processDefinitionQuery.count()).thenReturn(0L);

        runner.run(mock(org.springframework.boot.ApplicationArguments.class));

        verify(repositoryService, times(1)).createDeployment();
    }

    @Test
    @DisplayName("TC-02: BPMN key 已存在 → 跳过 (幂等)")
    void testSkipWhenKeyExists() {
        injectBpmns(makeBpmn(VALID_BPMN, "leave-approval.bpmn"));
        when(processDefinitionQuery.count()).thenReturn(1L);

        runner.run(mock(org.springframework.boot.ApplicationArguments.class));

        verify(repositoryService, never()).createDeployment();
    }

    @Test
    @DisplayName("TC-03: 缺 <bpmn:process id> → 跳过")
    void testSkipWhenNoProcessId() {
        String noProcess = "<?xml version=\"1.0\"?><bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"/>";
        injectBpmns(makeBpmn(noProcess, "no-process.bpmn"));

        runner.run(mock(org.springframework.boot.ApplicationArguments.class));

        verify(repositoryService, never()).createProcessDefinitionQuery();
        verify(repositoryService, never()).createDeployment();
    }

    @Test
    @DisplayName("TC-04: 非法 NCName processKey → 跳过")
    void testSkipWhenInvalidNCName() {
        String badKey = "<?xml version=\"1.0\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">\n" +
                "  <bpmn:process id=\"2121212212\" isExecutable=\"true\"/>\n" +
                "</bpmn:definitions>";
        injectBpmns(makeBpmn(badKey, "bad-key.bpmn"));

        runner.run(mock(org.springframework.boot.ApplicationArguments.class));

        verify(repositoryService, never()).createDeployment();
    }

    @Test
    @DisplayName("TC-05: 多个 BPMN 部分失败 → 其他仍正常处理")
    void testPartialFailure() {
        Resource good = makeBpmn(VALID_BPMN, "good.bpmn");
        String validBpmn2 = "<?xml version=\"1.0\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">\n" +
                "  <bpmn:process id=\"leave-approval-2\" isExecutable=\"true\"/>\n" +
                "</bpmn:definitions>";
        Resource good2 = makeBpmn(validBpmn2, "good2.bpmn");
        Resource bad = makeBpmn("not valid xml <<<", "bad.bpmn");
        injectBpmns(good, bad, good2);

        when(processDefinitionQuery.count()).thenReturn(0L);

        runner.run(mock(org.springframework.boot.ApplicationArguments.class));

        verify(repositoryService, times(2)).createDeployment();
    }

    @Test
    @DisplayName("TC-06: initBpmnResources 为空 → 直接返回")
    void testEmptyResources() {
        injectBpmns();

        runner.run(mock(org.springframework.boot.ApplicationArguments.class));

        verifyNoInteractions(repositoryService);
    }

    @Test
    @DisplayName("TC-07: NCName 边界 - 以 _ 开头的 key 合法")
    void testNCNameUnderscorePrefix() {
        String validUnderscore = "<?xml version=\"1.0\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">\n" +
                "  <bpmn:process id=\"_internal_process\" isExecutable=\"true\"/>\n" +
                "</bpmn:definitions>";
        injectBpmns(makeBpmn(validUnderscore, "underscore.bpmn"));
        when(processDefinitionQuery.count()).thenReturn(0L);

        runner.run(mock(org.springframework.boot.ApplicationArguments.class));

        verify(repositoryService, times(1)).createDeployment();
    }

    @Test
    @DisplayName("TC-08: RepositoryService 抛异常 → 不阻塞启动")
    void testRepositoryFailureDoesNotBlockStartup() {
        injectBpmns(makeBpmn(VALID_BPMN, "leave-approval.bpmn"));
        when(processDefinitionQuery.count()).thenThrow(new RuntimeException("DB connection lost"));

        assertDoesNotThrow(() -> runner.run(mock(org.springframework.boot.ApplicationArguments.class)));
    }
}