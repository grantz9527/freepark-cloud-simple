package com.freepark.cloud.simple.settings.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 响应序列化：库内 LocalDateTime 一律视为「UTC 锚点」，
 * 序列化前换算成系统配置时区的站点本地时间，再交给标准 jsr310 序列化器输出
 * （保证输出格式与改造前一致，仅时间数值随之区偏移）。
 */
public class SiteZoneLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    private final ObjectProvider<SiteZoneProvider> zoneProvider;

    public SiteZoneLocalDateTimeSerializer(ObjectProvider<SiteZoneProvider> zoneProvider) {
        this.zoneProvider = zoneProvider;
    }

    @Override
    public void serialize(LocalDateTime utcAnchor, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (utcAnchor == null) {
            gen.writeNull();
            return;
        }
        LocalDateTime siteWall = SiteZoneTimes.toSiteWall(utcAnchor, currentZone());
        LocalDateTimeSerializer.INSTANCE.serialize(siteWall, gen, serializers);
    }

    /** 懒解析时区（BeanPostProcessor 注册模块时提供者可能尚未创建，真正序列化时再取）。 */
    private ZoneId currentZone() {
        SiteZoneProvider provider = zoneProvider.getIfAvailable();
        return provider == null ? SiteZoneTimes.DEFAULT_ZONE : provider.currentZone();
    }
}
