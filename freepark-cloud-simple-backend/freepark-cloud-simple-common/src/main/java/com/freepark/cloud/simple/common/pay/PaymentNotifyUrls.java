package com.freepark.cloud.simple.common.pay;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * 支付异步通知 URL：系统默认（后台基础地址 + 固定路径）可覆盖为自定义绝对地址。
 */
public final class PaymentNotifyUrls {

    public static final int MAX_LENGTH = 512;

    private PaymentNotifyUrls() {
    }

    /**
     * 生效地址：已配置自定义则用自定义，否则用默认。
     */
    public static String resolve(String stored, String defaultUrl) {
        if (StringUtils.hasText(stored)) {
            return stored.trim();
        }
        return defaultUrl == null ? "" : defaultUrl.trim();
    }

    /**
     * 保存用：可空；与默认相同或空白则存空串（表示跟随默认）；
     * 否则须为 http(s) 绝对地址。
     */
    public static String normalizeForStore(String value, String defaultUrl) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String url = value.trim();
        String defaults = defaultUrl == null ? "" : defaultUrl.trim();
        if (!defaults.isEmpty() && url.equals(defaults)) {
            return "";
        }
        if (url.length() > MAX_LENGTH) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_NOTIFY_URL);
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_NOTIFY_URL);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || host.isBlank()
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || uri.getUserInfo() != null) {
            throw new BizException(400, MessageKeys.SETTINGS_INVALID_NOTIFY_URL);
        }
        return url;
    }
}
