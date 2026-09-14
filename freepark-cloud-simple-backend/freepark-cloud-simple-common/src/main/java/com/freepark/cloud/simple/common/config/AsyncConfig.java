package com.freepark.cloud.simple.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务：C 端查费后回写流水应收等不阻塞主请求的后台工作。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "parkingFeeSnapshotExecutor")
    public AsyncTaskExecutor parkingFeeSnapshotExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("fee-snap-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
