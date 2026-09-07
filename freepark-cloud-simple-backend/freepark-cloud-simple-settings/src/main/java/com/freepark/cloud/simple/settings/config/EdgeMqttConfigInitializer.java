package com.freepark.cloud.simple.settings.config;

import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.repository.EdgeMqttConfigRepository;
import com.freepark.cloud.simple.settings.support.EdgeMqttConfigOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化默认边缘计算（MQTT）配置（单例记录不存在时创建）。
 */
@Component
public class EdgeMqttConfigInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EdgeMqttConfigInitializer.class);

    private final EdgeMqttConfigRepository repository;

    public EdgeMqttConfigInitializer(EdgeMqttConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.existsById(EdgeMqttConfig.SINGLETON_ID)) {
            return;
        }
        EdgeMqttConfig config = new EdgeMqttConfig(
                EdgeMqttConfigOptions.DEFAULT_BROKER_HOST,
                EdgeMqttConfigOptions.DEFAULT_BROKER_PORT,
                EdgeMqttConfigOptions.DEFAULT_CLIENT_ID);
        repository.save(config);
        log.info("已初始化默认边缘计算配置：host={}, port={}, clientId={}",
                config.getBrokerHost(), config.getBrokerPort(), config.getClientId());
    }
}
