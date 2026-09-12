package com.freepark.cloud.simple.common.pay;

/**
 * 后台基础地址（云端 API 公网根，无尾斜杠）：由系统配置提供，用于支付回调。
 * 底层模块不直接依赖 settings，只依赖本接口。
 */
public interface SiteBaseUrlProvider {

    /**
     * 当前配置的后台基础地址；未填写时返回空串，永不为 null。
     */
    String currentBaseUrl();
}
