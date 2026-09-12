package com.freepark.cloud.simple.common.pay;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 云端公网根地址解析：后台地址用于支付回调，用户端地址用于首页/缴费页跳回。
 * <p>
 * 后台优先级：系统配置「后台基础地址」→ {@code freepark.public.base-url} → 当前请求 Host。<br>
 * 用户端优先级：系统配置「用户端基础地址」→ {@code freepark.public.user-base-url}
 * → 后台基础地址 → 当前请求 Host。
 */
@Component
public class PublicOriginResolver {

    private final ObjectProvider<SiteBaseUrlProvider> adminBaseUrl;
    private final ObjectProvider<UserBaseUrlProvider> userBaseUrl;
    private final String configuredAdminBaseUrl;
    private final String configuredUserBaseUrl;

    public PublicOriginResolver(ObjectProvider<SiteBaseUrlProvider> adminBaseUrl,
                                ObjectProvider<UserBaseUrlProvider> userBaseUrl,
                                @Value("${freepark.public.base-url:}") String configuredAdminBaseUrl,
                                @Value("${freepark.public.user-base-url:}") String configuredUserBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
        this.userBaseUrl = userBaseUrl;
        this.configuredAdminBaseUrl = configuredAdminBaseUrl;
        this.configuredUserBaseUrl = configuredUserBaseUrl;
    }

    /** 后台（API）公网根，用于支付异步通知。 */
    public String origin(HttpServletRequest request) {
        SiteBaseUrlProvider provider = adminBaseUrl.getIfAvailable();
        if (provider != null && StringUtils.hasText(provider.currentBaseUrl())) {
            return PublicBaseUrls.trimSlash(provider.currentBaseUrl());
        }
        if (StringUtils.hasText(configuredAdminBaseUrl)) {
            return PublicBaseUrls.trimSlash(configuredAdminBaseUrl);
        }
        return requestOrigin(request);
    }

    /** 用户端公网根（可含子路径），用于首页与缴费结果页。 */
    public String userOrigin(HttpServletRequest request) {
        UserBaseUrlProvider provider = userBaseUrl.getIfAvailable();
        if (provider != null && StringUtils.hasText(provider.currentBaseUrl())) {
            return PublicBaseUrls.trimSlash(provider.currentBaseUrl());
        }
        if (StringUtils.hasText(configuredUserBaseUrl)) {
            return PublicBaseUrls.trimSlash(configuredUserBaseUrl);
        }
        // 未单独配置用户端时回落后台地址，兼容旧部署
        return origin(request);
    }

    public String wechatNotifyUrl(HttpServletRequest request) {
        return PublicBaseUrls.join(origin(request), PaymentNotifyPaths.WECHAT);
    }

    public String alipayNotifyUrl(HttpServletRequest request) {
        return PublicBaseUrls.join(origin(request), PaymentNotifyPaths.ALIPAY);
    }

    /** 用户端缴款结果页绝对地址，供支付宝/微信同步跳回。 */
    public String userPayReturnUrl(HttpServletRequest request, String payNo) {
        String no = payNo == null ? "" : payNo.trim();
        return PublicBaseUrls.join(userOrigin(request), PaymentNotifyPaths.USER_PAY_PREFIX + no);
    }

    private static String requestOrigin(HttpServletRequest request) {
        String proto = firstForwarded(request.getHeader("X-Forwarded-Proto"));
        if (!StringUtils.hasText(proto)) {
            proto = request.getScheme();
        }
        String host = firstForwarded(request.getHeader("X-Forwarded-Host"));
        if (!StringUtils.hasText(host)) {
            host = request.getHeader("Host");
        }
        if (!StringUtils.hasText(host)) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(proto) && port == 80)
                    || ("https".equalsIgnoreCase(proto) && port == 443);
            if (!defaultPort && port > 0) {
                host = host + ":" + port;
            }
        }
        return proto + "://" + host;
    }

    private static String firstForwarded(String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        int comma = header.indexOf(',');
        String first = comma < 0 ? header : header.substring(0, comma);
        return first.trim();
    }
}
