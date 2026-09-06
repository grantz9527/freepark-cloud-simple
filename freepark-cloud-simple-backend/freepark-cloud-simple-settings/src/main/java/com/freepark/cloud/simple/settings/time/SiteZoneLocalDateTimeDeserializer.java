package com.freepark.cloud.simple.settings.time;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 请求反序列化：请求 JSON 中的 LocalDateTime 一律视为「系统配置时区的站点本地时间」，
 * 先用标准 jsr310 反序列化器解析（保证可解析格式与改造前一致），再换算成 UTC 锚点入库。
 */
public class SiteZoneLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private final ObjectProvider<SiteZoneProvider> zoneProvider;

    public SiteZoneLocalDateTimeDeserializer(ObjectProvider<SiteZoneProvider> zoneProvider) {
        this.zoneProvider = zoneProvider;
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        LocalDateTime siteWall = LocalDateTimeDeserializer.INSTANCE.deserialize(parser, context);
        if (siteWall == null) {
            return null;
        }
        return SiteZoneTimes.toUtcAnchor(siteWall, currentZone());
    }

    /** 懒解析时区（BeanPostProcessor 注册模块时提供者可能尚未创建，真正反序列化时再取）。 */
    private ZoneId currentZone() {
        SiteZoneProvider provider = zoneProvider.getIfAvailable();
        return provider == null ? SiteZoneTimes.DEFAULT_ZONE : provider.currentZone();
    }
}
