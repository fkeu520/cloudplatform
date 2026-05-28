package com.cloudhub.platform.ops.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SampleJob {

    @XxlJob("demoJobHandler")
    public void demoJob() {
        log.info("[XXL-JOB] 执行示例任务: 清理过期数据");
    }

    @XxlJob("cleanExpiredLogs")
    public void cleanExpiredLogs() {
        log.info("[XXL-JOB] 清理过期日志任务: 执行中");
    }
}
