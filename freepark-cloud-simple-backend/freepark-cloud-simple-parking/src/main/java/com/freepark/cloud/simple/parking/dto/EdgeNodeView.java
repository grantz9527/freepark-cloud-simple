package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.EdgeNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 边缘节点视图：节点基础信息 + 名下绑定的车场清单。
 *
 * @param boundLots 该节点名下车场（按车场编码排序；可为空）
 */
public record EdgeNodeView(
        Long id,
        String code,
        String name,
        boolean enabled,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BoundLot> boundLots) {

    public static EdgeNodeView from(EdgeNode node, List<ParkingLot> boundLots) {
        List<BoundLot> lots = boundLots.stream()
                .map(lot -> new BoundLot(lot.getCode(), lot.getName()))
                .toList();
        return new EdgeNodeView(
                node.getId(),
                node.getCode(),
                node.getName(),
                node.isEnabled(),
                node.getRemark(),
                node.getCreatedAt(),
                node.getUpdatedAt(),
                lots);
    }

    /**
     * 名下单个车场的摘要信息。
     *
     * @param parkCode 车场编码
     * @param parkName 车场名称
     */
    public record BoundLot(String parkCode, String parkName) {
    }
}
