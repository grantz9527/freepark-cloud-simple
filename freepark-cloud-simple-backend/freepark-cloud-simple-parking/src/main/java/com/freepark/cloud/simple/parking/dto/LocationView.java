package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingLocation;

/**
 * 车位位置视图（车场下的一级区域，如楼层）。
 */
public record LocationView(Long id, String name) {

    public static LocationView from(ParkingLocation location) {
        return new LocationView(location.getId(), location.getName());
    }
}
