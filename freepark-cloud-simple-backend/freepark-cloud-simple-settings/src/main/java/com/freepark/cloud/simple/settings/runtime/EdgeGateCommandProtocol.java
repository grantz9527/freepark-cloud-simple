package com.freepark.cloud.simple.settings.runtime;

/**
 * 边缘道闸指令协议 {@code edge.gate.command/1}（cloud → edge）。
 *
 * <p>缴费成功后云端向节点专属主题 {@code {commandPublishPrefix}/{nodeCode}} 发布
 * 一条开闸指令；边缘按车牌匹配「欠费拦截、正在等待缴费」的道闸并主动下发开闸。</p>
 */
public final class EdgeGateCommandProtocol {

    private EdgeGateCommandProtocol() {
    }

    public static final String SCHEMA = "edge.gate.command/1";

    /** 开闸 */
    public static final String COMMAND_OPEN = "OPEN";

    /** 因在线缴费成功而开闸 */
    public static final String REASON_PAYMENT = "PAYMENT";
}
