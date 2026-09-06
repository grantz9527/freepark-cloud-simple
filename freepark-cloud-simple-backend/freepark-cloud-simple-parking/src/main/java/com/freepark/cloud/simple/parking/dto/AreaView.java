package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingArea;

/**
 * 停车区域视图（位置下的区域）。
 */
public record AreaView(Long id, Long locationId, String name) {

    public static AreaView from(ParkingArea area) {
        return new AreaView(area.getId(), area.getLocation().getId(), area.getName());
    }
}
