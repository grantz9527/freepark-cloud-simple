package com.freepark.cloud.simple.settings.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.settings.time.SiteZoneLocalDateTimeDeserializer;
import com.freepark.cloud.simple.settings.time.SiteZoneLocalDateTimeSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 全局 LocalDateTime 时区换算：在 Spring Boot 组装好主 ObjectMapper 之后，
 * 最后注册本模块，确保对 {@link LocalDateTime} 的序列化/反序列化覆盖生效。
 * <p>
 * 统一时间语义：<b>请求侧 LocalDateTime = 系统配置时区的站点本地时间（↔ 换算后入库的 UTC 锚点）</b>；
 * <b>响应侧实体/View 中的 LocalDateTime = 库内 UTC 锚点（↔ 换算后输出站点本地时间）</b>。
 * 因此切系统时区后，所有接口返回的时间会自动随之区整体偏移，无需各 Service 手工换算。
 */
@Configuration(proxyBeanMethods = false)
public class SiteZoneJacksonConfig {

    @Bean
    public static BeanPostProcessor siteZoneJacksonModuleRegistrar(
            ObjectProvider<SiteZoneProvider> siteZoneProvider) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof ObjectMapper objectMapper) {
                    // 无条件注册；时区提供者由序列化/反序列化时懒解析，避免 Bean 创建顺序问题
                    objectMapper.registerModule(buildSiteZoneModule(siteZoneProvider));
                }
                return bean;
            }
        };
    }

    private static Module buildSiteZoneModule(ObjectProvider<SiteZoneProvider> siteZoneProvider) {
        SimpleModule module = new SimpleModule("SiteZoneLocalDateTime");
        module.addSerializer(LocalDateTime.class, new SiteZoneLocalDateTimeSerializer(siteZoneProvider));
        module.addDeserializer(LocalDateTime.class, new SiteZoneLocalDateTimeDeserializer(siteZoneProvider));
        return module;
    }
}
