package com.freepark.cloud.simple.parking.dto;

import java.util.List;

/**
 * 调整边缘节点名下绑定车场请求（全量替换语义）。
 *
 * @param parkCodes 该节点应管辖的车场编码集合；不在集合中的原绑定车场将被解绑
 */
public record UpdateEdgeNodeLotsRequest(
        List<String> parkCodes) {
}
