package com.freepark.cloud.simple.settings.runtime;

/**
 * 边缘道闸指令协议 {@code edge.gate.command/1}（cloud → edge）。
 *
 * <p>欠费拦截不上报离场：云端在识别算费时记下通道等待。缴费成功后向
 * {@code {commandPublishPrefix}/{nodeCode}} 发布开闸（带车牌；有等待则带 {@code laneCode}）。
 * 边缘按该通道最新欠费拦截识别开闸并播报，不要扫全部道闸。</p>
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
