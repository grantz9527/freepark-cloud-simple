package com.freepark.cloud.simple.common.auth;

/**
 * 当前登录用户上下文（基于 ThreadLocal，请求结束由拦截器清理）。
 */
public final class AuthContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(String username) {
        HOLDER.set(username);
    }

    /** 返回当前登录用户名，未登录时为 null */
    public static String getUsername() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
