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

    /**
     * 站点默认车牌版式（国家/地区）：决定 C 端（用户端网页）默认采用的车牌输入 UI。
     * 各取值与用户端前端 plateRegion.ts 中的版式预设一一对应（CN 之外均含本地版式分段 + 自由输入回退）。
     */
    public static final String DEFAULT_PLATE_REGION = "CN";

    public static final List<String> SUPPORTED_PLATE_REGIONS = List.of(
            "CN", "HK", "MO", "TW", "EU", "GB", "US", "JP", "KR", "SG");

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
     * 币种：ISO 4217 货币代码预置列表，勾选启用后作为站点“收费金额单位”的可选项。
     */
    public static final List<String> SUPPORTED_CURRENCIES = List.of(
            "CNY", "USD", "HKD", "TWD", "MOP", "JPY", "KRW", "SGD", "MYR", "THB", "VND",
            "EUR", "GBP", "CHF", "AUD", "CAD", "NZD");

    /** 默认启用的常见币种 */
    public static final List<String> DEFAULT_ALLOWED_CURRENCIES = List.of(
            "CNY", "USD", "HKD", "JPY", "EUR", "GBP", "SGD", "AUD", "CAD");

    /** 默认币种（收费金额单位）：人民币 */
    public static final String DEFAULT_CURRENCY = "CNY";

    /**
     * 收费方式：站点支持的缴费/收款渠道预置列表，勾选启用。
     */
    public static final List<String> SUPPORTED_PAYMENT_METHODS = List.of("WECHAT_PAY", "ALIPAY_PAY");

    /** 默认启用的收费方式 */
    public static final List<String> DEFAULT_ALLOWED_PAYMENT_METHODS = List.of("WECHAT_PAY");

    /** 基础地址最大长度（scheme + host + 可选端口 + 部署子路径） */
    public static final int MAX_SITE_BASE_URL_LENGTH = 255;

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

    /**
     * 公网基础地址（后台 / 用户端共用校验）：可空；填写时必须是 http(s) 地址，
     * 允许端口与部署子路径（如 /fangzhi、/freepark-user）。
     * 查询串、片段、尾斜杠、空白会在保存时去掉，不再因此拦截。
     * 未填写返回空串。
     */
    public static String validateSiteBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String raw = sanitizeSiteBaseInput(value);
        if (raw.isEmpty()) {
            return "";
        }
        String scheme;
        String rest;
        int schemeSep = raw.indexOf("://");
        if (schemeSep <= 0) {
            scheme = "https";
            rest = raw.startsWith("//") ? raw.substring(2) : raw;
        } else {
            scheme = raw.substring(0, schemeSep).toLowerCase();
            rest = raw.substring(schemeSep + 3);
        }
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw invalidSiteBaseUrl();
        }
        int at = rest.lastIndexOf('@');
        if (at >= 0) {
            rest = rest.substring(at + 1);
        }
        int cut = rest.length();
        int queryAt = indexOfAny(rest, '?', '#');
        if (queryAt >= 0) {
            cut = queryAt;
        }
        rest = stripTrailingSlashes(rest.substring(0, cut));
        if (rest.isEmpty() || rest.startsWith("/")) {
            throw invalidSiteBaseUrl();
        }
        int slash = rest.indexOf('/');
        String authority = slash < 0 ? rest : rest.substring(0, slash);
        String path = slash < 0 ? "" : stripTrailingSlashes(rest.substring(slash));
        if (authority.isBlank() || path.contains("..") || !isValidSiteAuthority(authority)) {
            throw invalidSiteBaseUrl();
        }
        String result = scheme + "://" + authority + path;
        if (result.length() > MAX_SITE_BASE_URL_LENGTH) {
            throw invalidSiteBaseUrl();
        }
        return result;
    }

    private static BizException invalidSiteBaseUrl() {
        return new BizException(400, MessageKeys.SETTINGS_INVALID_SITE_BASE_URL);
    }

    private static String sanitizeSiteBaseInput(String value) {
        String raw = value.trim()
                .replace('\u3000', ' ')
                .replace('\uFF1A', ':')
                .replace('\uFF0F', '/')
                .replace('\\', '/')
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .trim();
        if (raw.length() >= 2) {
            char first = raw.charAt(0);
            char last = raw.charAt(raw.length() - 1);
            if ((first == '"' && last == '"')
                    || (first == '\'' && last == '\'')
                    || (first == '“' && last == '”')
                    || (first == '‘' && last == '’')
                    || (first == '<' && last == '>')) {
                raw = raw.substring(1, raw.length() - 1).trim();
            }
        }
        raw = raw.replaceAll("\\s+", "");
        return stripTrailingSlashes(raw);
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static int indexOfAny(String value, char a, char b) {
        int ia = value.indexOf(a);
        int ib = value.indexOf(b);
        if (ia < 0) {
            return ib;
        }
        if (ib < 0) {
            return ia;
        }
        return Math.min(ia, ib);
    }

    private static boolean isValidSiteAuthority(String authority) {
        if (authority.startsWith("[")) {
            int close = authority.indexOf(']');
            if (close < 2) {
                return false;
            }
            String after = authority.substring(close + 1);
            return after.isEmpty() || isNumericPort(after);
        }
        int colon = authority.lastIndexOf(':');
        String host = colon < 0 ? authority : authority.substring(0, colon);
        String port = colon < 0 ? "" : authority.substring(colon);
        if (host.isBlank()) {
            return false;
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') {
                continue;
            }
            return false;
        }
        return port.isEmpty() || isNumericPort(port);
    }

    private static boolean isNumericPort(String value) {
        if (value.length() < 2 || value.charAt(0) != ':') {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
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

    public static String validatePlateRegion(String value) {
        String region = normalize(value);
        if (!SUPPORTED_PLATE_REGIONS.contains(region)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return region;
    }

    public static String validatePlateColor(String value) {
        String color = normalize(value);
        if (!SUPPORTED_PLATE_COLORS.contains(color)) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_PLATE_COLOR);
        }
        return color;
    }

    public static String validateCurrency(String value) {
        String currency = normalize(value);
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_CURRENCY);
        }
        return currency;
    }

    public static String validatePaymentMethod(String value) {
        String method = normalize(value);
        if (!SUPPORTED_PAYMENT_METHODS.contains(method)) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_PAYMENT_METHOD);
        }
        return method;
    }

    /**
     * 去重并校验允许收费方式列表，至少需要一种。
     */
    public static List<String> normalizeAllowedPaymentMethods(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new BizException(400, MessageKeys.SETTINGS_EMPTY_PAYMENT_METHODS);
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String method = validatePaymentMethod(value);
            if (!result.contains(method)) {
                result.add(method);
            }
        }
        if (result.isEmpty()) {
            throw new BizException(400, MessageKeys.SETTINGS_EMPTY_PAYMENT_METHODS);
        }
        return result;
    }

    /**
     * 去重并校验允许币种列表，至少需要一种。
     */
    public static List<String> normalizeAllowedCurrencies(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new BizException(400, MessageKeys.SETTINGS_EMPTY_CURRENCIES);
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String currency = validateCurrency(value);
            if (!result.contains(currency)) {
                result.add(currency);
            }
        }
        if (result.isEmpty()) {
            throw new BizException(400, MessageKeys.SETTINGS_EMPTY_CURRENCIES);
        }
        return result;
    }

    /**
     * 默认币种必须落在允许列表中。
     */
    public static void ensureDefaultCurrencyAllowed(String defaultCurrency, List<String> allowed) {
        if (defaultCurrency == null || allowed == null || !allowed.contains(defaultCurrency)) {
            throw new BizException(400, MessageKeys.SETTINGS_DEFAULT_CURRENCY_NOT_ALLOWED);
        }
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
