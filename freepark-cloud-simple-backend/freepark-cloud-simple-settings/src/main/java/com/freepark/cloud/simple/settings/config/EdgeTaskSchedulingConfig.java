package com.freepark.cloud.simple.settings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用 Spring 定时任务：供边缘计算配置周期下发等后台调度使用。
 * 调度组件注册在 settings 模块内，由外层 startup 统一装配运行。
 */
@Configuration
@EnableScheduling
public class EdgeTaskSchedulingConfig {
}
