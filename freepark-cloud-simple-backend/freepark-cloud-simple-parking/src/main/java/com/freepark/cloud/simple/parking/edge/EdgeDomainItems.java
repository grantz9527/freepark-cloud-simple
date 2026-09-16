package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.parking.entity.AccessJudgmentRuleType;
import com.freepark.cloud.simple.parking.entity.BlacklistVehicle;
import com.freepark.cloud.simple.parking.entity.InternalVehicle;
import com.freepark.cloud.simple.parking.entity.ParkingArea;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLocation;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingSpace;
import com.freepark.cloud.simple.parking.entity.PatternAllowlist;
import com.freepark.cloud.simple.parking.entity.WhitelistVehicle;

/**
 * 各可同步业务域的“单条快照条目”序列化共用小工具（全量快照与增量帧共用）。
 *
 * <p>同一实体的条目 JSON 形状在此集中定义：域快照构建器（EdgeConfigDomainSnapshotProvider）
 * 组装整表全量时逐行调用，增量发布器组装单条变更条目时也调用同一方法，
 * 保证“全量替换”与“按 cloud_id 增删改”两种边缘应用路径拿到的条目结构完全一致。</p>
 *
 * <p>字段约定沿用 {@link EdgeSnapshotJson}：可空字符串/时间为空时直接省略（缺省即空），
 * 枚举输出 .name()，时间为 UTC 本地时刻文本（无时区后缀）。条目恒携带云端主键 id，
 * 边缘侧以它为 cloud_id 做 upsert/delete 定位。</p>
 */
final class EdgeDomainItems {

    private EdgeDomainItems() {
    }

    /** 扁平条目流中的行类型（space 域） */
    static final String TYPE_LOCATION = "location";
    static final String TYPE_AREA = "area";
    static final String TYPE_SPACE = "space";

    // ---------- 车场（lot）：条目即车场整体运行配置 ----------

    static ObjectNode lot(ObjectMapper mapper, ParkingLot lot) {
        ObjectNode node = mapper.createObjectNode();
        node.put("code", lot.getCode());
        node.put("name", lot.getName());
        node.put("lotType", lot.getLotType().name());
        node.put("enabled", lot.isEnabled());
        node.put("entryInterceptArrears", lot.isEntryInterceptArrears());
        node.put("entryInterceptBlacklist", lot.isEntryInterceptBlacklist());
        node.put("entryInterceptFull", lot.isEntryInterceptFull());
        node.put("exitInterceptArrears", lot.isExitInterceptArrears());
        node.put("exitInterceptBlacklist", lot.isExitInterceptBlacklist());
        ArrayNode judgmentOrder = node.putArray("judgmentOrder");
        for (AccessJudgmentRuleType rule : lot.effectiveJudgmentOrder()) {
            judgmentOrder.add(rule.name());
        }
        node.put("updatedAt", lot.getUpdatedAt().toString());
        return node;
    }

    // ---------- 黑名单 ----------

    static ObjectNode blacklist(ObjectMapper mapper, BlacklistVehicle vehicle) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", vehicle.getId());
        node.put("plateNumber", vehicle.getPlateNumber());
        node.put("plateColor", vehicle.getPlateColor().name());
        node.put("ownerName", vehicle.getOwnerName());
        EdgeSnapshotJson.putIfPresent(node, "phone", vehicle.getPhone());
        EdgeSnapshotJson.putIfPresent(node, "department", vehicle.getDepartment());
        EdgeSnapshotJson.putIfPresent(node, "remark", vehicle.getRemark());
        EdgeSnapshotJson.putTimeIfPresent(node, "startTime", vehicle.getStartTime());
        EdgeSnapshotJson.putTimeIfPresent(node, "endTime", vehicle.getEndTime());
        node.put("enabled", vehicle.isEnabled());
        return node;
    }

    // ---------- 正则名单（放行名单规则） ----------

    static ObjectNode pattern(ObjectMapper mapper, PatternAllowlist rule) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", rule.getId());
        node.put("name", rule.getName());
        node.put("pattern", rule.getPattern());
        EdgeSnapshotJson.putIfPresent(node, "remark", rule.getRemark());
        node.put("enabled", rule.isEnabled());
        return node;
    }

    // ---------- 白名单 ----------

    static ObjectNode whitelist(ObjectMapper mapper, WhitelistVehicle vehicle) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", vehicle.getId());
        node.put("plateNumber", vehicle.getPlateNumber());
        node.put("plateColor", vehicle.getPlateColor().name());
        node.put("ownerName", vehicle.getOwnerName());
        node.put("type", vehicle.getType().name());
        EdgeSnapshotJson.putIfPresent(node, "phone", vehicle.getPhone());
        EdgeSnapshotJson.putIfPresent(node, "department", vehicle.getDepartment());
        EdgeSnapshotJson.putIfPresent(node, "remark", vehicle.getRemark());
        EdgeSnapshotJson.putTimeIfPresent(node, "startTime", vehicle.getStartTime());
        EdgeSnapshotJson.putTimeIfPresent(node, "endTime", vehicle.getEndTime());
        node.put("enabled", vehicle.isEnabled());
        return node;
    }

    // ---------- 内部车辆 ----------

    static ObjectNode internal(ObjectMapper mapper, InternalVehicle vehicle) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", vehicle.getId());
        node.put("plateNumber", vehicle.getPlateNumber());
        node.put("plateColor", vehicle.getPlateColor().name());
        node.put("ownerName", vehicle.getOwnerName());
        node.put("type", vehicle.getType().name());
        EdgeSnapshotJson.putIfPresent(node, "phone", vehicle.getPhone());
        EdgeSnapshotJson.putIfPresent(node, "department", vehicle.getDepartment());
        EdgeSnapshotJson.putIfPresent(node, "remark", vehicle.getRemark());
        EdgeSnapshotJson.putIfPresent(node, "batchId", vehicle.getBatchId());
        node.put("enabled", vehicle.isEnabled());
        return node;
    }

    // ---------- 车位管理（位置/区域/车位 三层扁平条目） ----------

    static ObjectNode location(ObjectMapper mapper, ParkingLocation location) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", TYPE_LOCATION);
        node.put("id", location.getId());
        node.put("name", location.getName());
        return node;
    }

    static ObjectNode area(ObjectMapper mapper, ParkingArea area) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", TYPE_AREA);
        node.put("id", area.getId());
        node.put("name", area.getName());
        node.put("locationId", area.getLocation().getId());
        return node;
    }

    static ObjectNode space(ObjectMapper mapper, ParkingSpace space) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", TYPE_SPACE);
        node.put("id", space.getId());
        node.put("code", space.getCode());
        node.put("enabled", space.isEnabled());
        node.put("areaId", space.getArea().getId());
        return node;
    }

    // ---------- 通道（出入口） ----------

    static ObjectNode lane(ObjectMapper mapper, ParkingLane lane) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", lane.getId());
        node.put("name", lane.getName());
        node.put("code", lane.getCode());
        node.put("laneType", lane.getLaneType().name());
        node.put("enabled", lane.isEnabled());
        if (lane.getLinkedLot() != null) {
            EdgeSnapshotJson.putIfPresent(node, "linkedLotCode", lane.getLinkedLot().getCode());
        }
        return node;
    }
}
