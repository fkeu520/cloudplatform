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
