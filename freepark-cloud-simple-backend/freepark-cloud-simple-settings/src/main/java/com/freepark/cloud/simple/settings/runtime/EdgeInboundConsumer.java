package com.freepark.cloud.simple.settings.runtime;

/**
 * 边缘 MQTT 入站消息消费方：接收云端订阅主题（上行数据/心跳等）送达的消息。
 *
 * <p>接口定义在 settings（不感知具体业务实体）；由 settings 内部或外层 startup
 * 提供实现并注册为 Spring Bean。连接管理器把“订阅主题上收到的每条消息”扇出给
 * 全部消费方，由消费方自行校验 topic/payload 的相关性。</p>
 */
public interface EdgeInboundConsumer {

    /**
     * 处理一条订阅主题送达的消息。
     *
     * @param topic   消息主题（已由云端订阅，按订阅过滤器的命中消息）
     * @param payload 消息原始字节（UTF-8 JSON 负载由实现方自行解析）
     */
    void onMessage(String topic, byte[] payload);
}
