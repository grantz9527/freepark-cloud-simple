package com.freepark.cloud.simple.parking.service;

/**
 * 渠道支付结果通知入账结果。
 */
public enum ChannelSettleResult {
    /** 本次入账成功 */
    OK,
    /** 已处理过或无需再改账（幂等成功） */
    DUPLICATE,
    /** 单号/金额/渠道不匹配，应让渠道重试或告警 */
    REJECT
}
