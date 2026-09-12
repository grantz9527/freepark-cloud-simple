package com.freepark.cloud.simple.common.pay;

import org.springframework.util.StringUtils;

/**
 * 公网基础地址（可含部署子路径）与相对路径拼接。
 */
public final class PublicBaseUrls {

    private PublicBaseUrls() {
    }

    /** 去掉末尾斜杠，保留中间路径（如 https://host/fangzhi）。 */
    public static String trimSlash(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '/') {
            end--;
        }
        return text.substring(0, end);
    }

    /** {@code base}（可含子路径）+ {@code path}（须以 / 开头的相对路径）。 */
    public static String join(String base, String path) {
        String root = trimSlash(base);
        String rel = path == null ? "" : path.trim();
        if (!StringUtils.hasText(rel)) {
            return root;
        }
        if (!rel.startsWith("/")) {
            rel = "/" + rel;
        }
        return root + rel;
    }
}
