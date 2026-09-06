package com.freepark.cloud.simple.settings.support;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 站点配置的受支持取值与默认值（参照 freepark local_server 的 SupportedLocale /
 * SupportedTimezone / PlateColorSupport 精简而来，仅覆盖当前前端/后端已支持范围）。
 */
public final class SystemSettingsOptions {

    private SystemSettingsOptions() {
    }

    /** 默认语言：管理端全局语言 zh-CN / en */
    public static final String DEFAULT_LOCALE = "zh-CN";

    public static final List<String> SUPPORTED_LOCALES = List.of("zh-CN", "en");

    /** 默认时区 */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    /** 支持选择的时区（与 local_server SupportedTimezone 一致） */
    public static final List<String> SUPPORTED_TIMEZONES = List.of(
            "UTC",
            "Asia/Shanghai",
            "Asia/Tokyo",
            "Asia/Seoul",
            "Asia/Singapore",
            "Asia/Hong_Kong",
            "Asia/Kolkata",
            "Europe/London",
            "Europe/Paris",
            "Europe/Berlin",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "Australia/Sydney");

    /**
     * 车牌颜色：覆盖全球常见车牌底色，与参考实现 freepark local_server 的
     * PlateColor 枚举保持一致（中国常用 蓝/黄/绿(新能源)/黄绿/黑/白）。
     */
    public static final List<String> SUPPORTED_PLATE_COLORS = List.of(
            "BLUE", "YELLOW", "GREEN", "YELLOW_GREEN", "BLACK", "WHITE",
            "RED", "ORANGE", "BROWN", "PURPLE", "PINK", "GRAY",
            "SILVER", "GOLD", "CREAM", "BEIGE", "NAVY", "MAROON",
            "OLIVE", "TEAL", "CYAN", "MAGENTA", "LIME", "LAVENDER",
            "TURQUOISE", "INDIGO", "CORAL", "AMBER", "VIOLET", "CHARCOAL",
            "LIGHT_BLUE", "LIGHT_GREEN", "DARK_BLUE", "DARK_GREEN", "RUST", "BRONZE",
            "PEACH", "MINT", "ROSE", "SALMON", "COPPER", "PLUM",
            "CRIMSON", "SCARLET", "EMERALD", "SAPPHIRE", "RUBY", "OTHER");

    /** 默认允许的中国常见车牌颜色（不含“其他”） */
    public static final List<String> DEFAULT_ALLOWED_PLATE_COLORS = List.of(
            "BLUE", "YELLOW", "GREEN", "YELLOW_GREEN", "BLACK", "WHITE");

    public static final String DEFAULT_PLATE_COLOR = "BLUE";

    /**
     * 将配置的时区字符串解析为 {@link ZoneId}；非法或缺失时回退到默认时区。
     */
    public static ZoneId zoneIdOrDefault(String value) {
        if (value != null && !value.isBlank()) {
            try {
                return ZoneId.of(value.trim());
            } catch (DateTimeException ignored) {
                // fall through to default
            }
        }
        return ZoneId.of(DEFAULT_TIMEZONE);
    }

    public static String validateLocale(String value) {
        String tag = normalize(value);
        if (!SUPPORTED_LOCALES.contains(tag)) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_LOCALE);
        }
        return tag;
    }

    public static String validateTimezone(String value) {
        String zone = normalize(value);
        if (!SUPPORTED_TIMEZONES.contains(zone)) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_TIMEZONE);
        }
        return zone;
    }

    public static String validatePlateColor(String value) {
        String color = normalize(value);
        if (!SUPPORTED_PLATE_COLORS.contains(color)) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_PLATE_COLOR);
        }
        return color;
    }

    /**
     * 去重并校验允许颜色列表，至少需要一种。
     */
    public static List<String> normalizeAllowed(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new BizException(400, MessageKeys.SETTINGS_EMPTY_PLATE_COLORS);
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String color = validatePlateColor(value);
            if (!result.contains(color)) {
                result.add(color);
            }
        }
        if (result.isEmpty()) {
            throw new BizException(400, MessageKeys.SETTINGS_EMPTY_PLATE_COLORS);
        }
        return result;
    }

    /**
     * 默认颜色必须落在允许列表中。
     */
    public static void ensureDefaultAllowed(String defaultColor, List<String> allowed) {
        if (defaultColor == null || allowed == null || !allowed.contains(defaultColor)) {
            throw new BizException(400, MessageKeys.SETTINGS_DEFAULT_COLOR_NOT_ALLOWED);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return value.trim();
    }
}
