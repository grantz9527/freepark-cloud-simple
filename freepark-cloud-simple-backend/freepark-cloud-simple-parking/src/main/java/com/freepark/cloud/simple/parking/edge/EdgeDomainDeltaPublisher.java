package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.parking.entity.BlacklistVehicle;
import com.freepark.cloud.simple.parking.entity.InternalVehicle;
import com.freepark.cloud.simple.parking.entity.ParkingArea;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLocation;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingSpace;
import com.freepark.cloud.simple.parking.entity.PatternAllowlist;
import com.freepark.cloud.simple.parking.entity.WhitelistVehicle;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncDispatcher;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 边缘配置域变更的 AFTER_COMMIT 增量发布器：业务事务提交后收到
 * {@link EdgeConfigDomainChangedEvent}，把变更集合序列化为 v3 kind=delta 的条目数组
 * （条目形状与全量快照完全一致，见 {@link EdgeDomainItems}），交给配置同步调度器
 * 分帧下发到管理该车场的全部节点。
 *
 * <p>仅在事务提交后执行，因此写事务回滚不会产生任何增量；增量在事务提交线程内
 * 同步完成，配合调度器内部锁可保证与周期全量、心跳恢复全量不交错。</p>
 */
@Component
public class EdgeDomainDeltaPublisher {

    private static final Logger log = LoggerFactory.getLogger(EdgeDomainDeltaPublisher.class);

    private final ObjectMapper objectMapper;
    private final EdgeConfigSyncDispatcher dispatcher;

    public EdgeDomainDeltaPublisher(ObjectMapper objectMapper, EdgeConfigSyncDispatcher dispatcher) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainChanged(EdgeConfigDomainChangedEvent event) {
        if (event == null || event.isEmpty()) {
            return;
        }
        try {
            ArrayNode entries = buildEntries(event.domain(), event.changes());
            if (entries.isEmpty()) {
                return;
            }
            dispatcher.publishDomainDelta(event.parkCode(), event.domain(), entries);
        } catch (RuntimeException e) {
            log.warn("边缘配置增量转换失败 lot={} domain={}：{}", event.parkCode(), event.domain(),
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private ArrayNode buildEntries(String domain, List<EdgeConfigDomainChangedEvent.Change> changes) {
        ArrayNode entries = objectMapper.createArrayNode();
        for (EdgeConfigDomainChangedEvent.Change change : changes) {
            ObjectNode entry = entries.addObject();
            entry.put("op", change.op());
            if (EdgeConfigSyncProtocol.DELTA_OP_UPSERT.equals(change.op())) {
                ObjectNode item = toItem(domain, change.item());
                if (item == null) {
                    entries.remove(entries.size() - 1);
                    continue;
                }
                entry.set("item", item);
            } else if (EdgeConfigSyncProtocol.DELTA_OP_DELETE.equals(change.op())) {
                if (!(change.item() instanceof Number number)) {
                    entries.remove(entries.size() - 1);
                    continue;
                }
                entry.put("id", number.longValue());
            } else {
                log.warn("忽略未知增量操作：{}（lot={} domain={}）", change.op(),
                        change.item(), domain);
                entries.remove(entries.size() - 1);
            }
        }
        return entries;
    }

    /** 把已保存的业务实体序列化为与全量快照同构的单条条目（按域分派） */
    private ObjectNode toItem(String domain, Object item) {
        if (item == null) {
            return null;
        }
        if (EdgeConfigSyncProtocol.DOMAIN_LOT.equals(domain)) {
            return item instanceof ParkingLot lot
                    ? EdgeDomainItems.lot(objectMapper, lot) : null;
        }
        if (EdgeConfigSyncProtocol.DOMAIN_BLACKLIST.equals(domain)) {
            return item instanceof BlacklistVehicle vehicle
                    ? EdgeDomainItems.blacklist(objectMapper, vehicle) : null;
        }
        if (EdgeConfigSyncProtocol.DOMAIN_PATTERN.equals(domain)) {
            return item instanceof PatternAllowlist rule
                    ? EdgeDomainItems.pattern(objectMapper, rule) : null;
        }
        if (EdgeConfigSyncProtocol.DOMAIN_WHITELIST.equals(domain)) {
            return item instanceof WhitelistVehicle vehicle
                    ? EdgeDomainItems.whitelist(objectMapper, vehicle) : null;
        }
        if (EdgeConfigSyncProtocol.DOMAIN_INTERNAL.equals(domain)) {
            return item instanceof InternalVehicle vehicle
                    ? EdgeDomainItems.internal(objectMapper, vehicle) : null;
        }
        if (EdgeConfigSyncProtocol.DOMAIN_SPACE.equals(domain)) {
            if (item instanceof ParkingSpace space) {
                return EdgeDomainItems.space(objectMapper, space);
            }
            if (item instanceof ParkingArea area) {
                return EdgeDomainItems.area(objectMapper, area);
            }
            if (item instanceof ParkingLocation location) {
                return EdgeDomainItems.location(objectMapper, location);
            }
            return null;
        }
        if (EdgeConfigSyncProtocol.DOMAIN_LANE.equals(domain)) {
            return item instanceof ParkingLane lane
                    ? EdgeDomainItems.lane(objectMapper, lane) : null;
        }
        log.warn("忽略未知业务域增量条目：domain={}", domain);
        return null;
    }
}
