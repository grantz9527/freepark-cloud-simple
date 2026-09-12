package com.freepark.cloud.simple.common.pay;

/**
 * 用户端基础地址（C 端网页公网根，无尾斜杠）：由系统配置提供。
 * 用于用户端首页、缴费结果页与渠道同步跳回。
 */
public interface UserBaseUrlProvider {

    /**
     * 当前配置的用户端基础地址；未填写时返回空串，永不为 null。
     */
    String currentBaseUrl();
}
