package com.cloudhub.platform.workflow.config;

import org.flowable.engine.*;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
public class FlowableConfig {

    /**
     * Flowable Spring 引擎配置
     *
     * 历史背景 (2026-06-11):
     *   原配置使用了 SpringProcessEngineConfiguration 默认构造的 IdmEngineConfigurator,
     *   该 configurator 启动后会在部署 userTask 时查 ACT_ID_USER 验证 candidate user
     *   存在性. 由于本项目用户存于 sys_user (而非 ACT_ID_USER), 导致部署的 BPMN 中
     *   <flowable:candidateUsers>1</flowable:candidateUsers> 静默失效:
     *     - 任务创建时 ACT_RU_IDENTITYLINK 无 candidate 行
     *     - WorkflowTaskService.todoPage 查 candidate taskIds=[] (用户看不到任务)
     *     - WorkflowMessageProducer 警告 no recipients (Kafka 通知丢失)
     *
     *   修复: config.setDisableIdmEngine(true) 完全禁用 IDM 引擎初始化.
     *         Flowable 写 candidate 时不再查 ACT_ID_USER, 直接写入 ACT_RU_IDENTITYLINK.
     *         本项目通过 UserService 自身管理用户, 不需要 Flowable IDM.
     */
    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(
            DataSource dataSource, Environment env) {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(dataSource);
        config.setTransactionManager(new DataSourceTransactionManager(dataSource));
        config.setDatabaseSchemaUpdate(
                env.getProperty("flowable.database-schema-update", "true"));
        config.setAsyncExecutorActivate(
                Boolean.parseBoolean(env.getProperty("flowable.async-executor-activate", "true")));
        config.setActivityFontName(
                env.getProperty("flowable.activity-font-name", "宋体"));
        config.setLabelFontName(
                env.getProperty("flowable.label-font-name", "宋体"));
        config.setJdbcMaxActiveConnections(20);
        config.setJdbcMaxIdleConnections(10);
        config.setJdbcMaxCheckoutTime(20000);
        // 完全禁用 IDM 引擎, 避免查 ACT_ID_USER/ACT_ID_GROUP
        // (本项目用户存于 sys_user, 不依赖 Flowable IDM)
        config.setDisableIdmEngine(true);
        // 2026-06-12 补充: 显式置 null 兜底
        // a13d491 commit message 写的是 setIdmEngineConfigurator(null), 但实际代码只 setDisableIdmEngine(true)
        // Flowable 6.8.1 单 setDisableIdmEngine(true) 不彻底 (SpringProcessEngineConfiguration
        // 在构造时已自动 new IdmEngineConfigurator, 后续 disable 标志检查时机晚, BpmnDeployer
        // 部署时仍会查 ACT_ID_USER, 找不到就静默丢 candidate link)
        // 双保险: 标志 + null, 确保 IDM 引擎完全跳过
        config.setIdmEngineConfigurator(null);
        return config;
    }

    @Bean
    public ProcessEngineFactoryBean processEngineFactoryBean(
            SpringProcessEngineConfiguration config) {
        ProcessEngineFactoryBean factory = new ProcessEngineFactoryBean();
        factory.setProcessEngineConfiguration(config);
        return factory;
    }

    @Bean
    public ProcessEngine processEngine(ProcessEngineFactoryBean factory) throws Exception {
        return factory.getObject();
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public IdentityService identityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }
}
