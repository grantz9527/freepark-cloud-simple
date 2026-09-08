package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.parking.dto.CreateEdgeNodeRequest;
import com.freepark.cloud.simple.parking.dto.EdgeNodeView;
import com.freepark.cloud.simple.parking.dto.UpdateEdgeNodeLotsRequest;
import com.freepark.cloud.simple.parking.dto.UpdateEdgeNodeRequest;
import com.freepark.cloud.simple.parking.entity.EdgeNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.EdgeNodeRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeNodeBindingChangedEvent;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 边缘节点服务：节点资料的增删改与“节点 ↔ 车场”绑定管理。
 *
 * <p>节点编号不可手工指定：新增时由云端按「FreePark + 年月日(站点时区) + 6 位随机数」
 * 自动生成，且创建后不允许修改（更新接口不含编号字段）。</p>
 *
 * <p>绑定语义：一个车场最多归属一个节点（车场→节点单向），一个节点可管辖 0..N 个
 * 车场（1:1 只是 N=1 的特例）。绑定结果由 settings 边缘下发调度器在每个同步周期
 * 按节点整体推送到边缘侧，无需边缘侧预知车场清单。</p>
 */
@Service
public class EdgeNodeService {

    private final EdgeNodeRepository nodes;
    private final ParkingLotRepository lots;
    private final AdminGuard adminGuard;
    private final SiteZoneProvider siteZoneProvider;
    private final ApplicationEventPublisher eventPublisher;

    public EdgeNodeService(EdgeNodeRepository nodes, ParkingLotRepository lots,
            AdminGuard adminGuard, SiteZoneProvider siteZoneProvider,
            ApplicationEventPublisher eventPublisher) {
        this.nodes = nodes;
        this.lots = lots;
        this.adminGuard = adminGuard;
        this.siteZoneProvider = siteZoneProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<EdgeNodeView> listNodes() {
        adminGuard.requireEnabledAdmin();
        List<EdgeNode> all = nodes.findAllByOrderByCreatedAtDesc();
        if (all.isEmpty()) {
            return List.of();
        }
        // 一次取出全部车场，按所属节点编码分组（保证列表内车场按编码有序）
        Map<String, List<ParkingLot>> lotsByNode = new LinkedHashMap<>();
        List<ParkingLot> ordered = lots.findAllByOrderByCreatedAtDesc();
        ordered.sort((a, b) -> a.getCode().compareTo(b.getCode()));
        for (ParkingLot lot : ordered) {
            if (lot.getEdgeNodeCode() == null || lot.getEdgeNodeCode().isBlank()) {
                continue;
            }
            lotsByNode.computeIfAbsent(lot.getEdgeNodeCode(), k -> new ArrayList<>()).add(lot);
        }
        return all.stream()
                .map(node -> EdgeNodeView.from(node,
                        lotsByNode.getOrDefault(node.getCode(), List.of())))
                .toList();
    }

    @Transactional
    public EdgeNodeView createNode(CreateEdgeNodeRequest request) {
        requireSuperAdmin();
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        String name = normalizeRequired(request.name(), "name");
        if (name.length() > EdgeNode.MAX_NAME_LENGTH) {
            throw new BizException(400, MessageKeys.EDGE_NODE_NAME_TOO_LONG);
        }
        EdgeNode node = new EdgeNode();
        // 编号不可手工指定：按「FreePark + 年月日(站点时区) + 6 位随机数」自动生成
        node.setCode(generateNodeCode());
        node.setName(name);
        node.setEnabled(request.enabled() == null || request.enabled());
        node.setRemark(normalizeOptional(request.remark(), EdgeNode.MAX_REMARK_LENGTH));
        return EdgeNodeView.from(nodes.save(node), List.of());
    }

    /**
     * 生成唯一节点编号：FreePark + yyyyMMdd（站点时区当日）+ 6 位随机数。
     * <p>随机数允许前导零；极小概率撞号时重试，最多 5 次仍失败则报错。</p>
     */
    private String generateNodeCode() {
        ZoneId zone = siteZoneProvider == null ? SiteZoneTimes.DEFAULT_ZONE : siteZoneProvider.currentZone();
        LocalDateTime siteNow = SiteZoneTimes.toSiteWall(SiteZoneTimes.nowUtc(), zone);
        String prefix = "FreePark" + siteNow.format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = prefix + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            if (!nodes.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new BizException(500, MessageKeys.COMMON_SERVER_ERROR);
    }

    @Transactional
    public EdgeNodeView updateNode(Long nodeId, UpdateEdgeNodeRequest request) {
        requireSuperAdmin();
        EdgeNode node = requireNode(nodeId);
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        String name = normalizeRequired(request.name(), "name");
        if (name.length() > EdgeNode.MAX_NAME_LENGTH) {
            throw new BizException(400, MessageKeys.EDGE_NODE_NAME_TOO_LONG);
        }
        boolean wasEnabled = node.isEnabled();
        node.setName(name);
        if (request.enabled() != null) {
            node.setEnabled(request.enabled());
        }
        if (request.remark() != null) {
            node.setRemark(normalizeOptional(request.remark(), EdgeNode.MAX_REMARK_LENGTH));
        }
        EdgeNodeView view = EdgeNodeView.from(nodes.save(node), boundLots(node.getCode()));
        // 启用状态翻转会改变该节点是否属于同步目标：启用即补全量，停用即空快照清理
        if (request.enabled() != null && request.enabled() != wasEnabled) {
            publishBindingChanged(node.getCode());
        }
        return view;
    }

    @Transactional
    public EdgeNodeView bindLots(Long nodeId, UpdateEdgeNodeLotsRequest request) {
        requireSuperAdmin();
        EdgeNode node = requireNode(nodeId);
        List<String> rawCodes = request == null || request.parkCodes() == null
                ? List.of() : request.parkCodes();
        // 归一化：去空白、去重、保持稳定顺序
        Set<String> desired = new LinkedHashSet<>();
        for (String raw : rawCodes) {
            if (raw != null && !raw.isBlank()) {
                desired.add(raw.trim());
            }
        }

        Map<String, ParkingLot> lotByCode = new LinkedHashMap<>();
        for (ParkingLot lot : lots.findAll()) {
            lotByCode.put(lot.getCode(), lot);
        }
        List<String> unknown = new ArrayList<>();
        for (String code : desired) {
            if (!lotByCode.containsKey(code)) {
                unknown.add(code);
            }
        }
        if (!unknown.isEmpty()) {
            throw new BizException(400, MessageKeys.EDGE_NODE_LOT_NOT_FOUND,
                    String.join(", ", unknown));
        }

        // 全量替换：目标车场挂到本节点；原属本节点但不在新清单内的车场解绑
        List<ParkingLot> toSave = new ArrayList<>();
        Set<String> affectedCodes = new LinkedHashSet<>();
        for (ParkingLot lot : lotByCode.values()) {
            boolean wanted = desired.contains(lot.getCode());
            String previousNode = lot.getEdgeNodeCode();
            if (wanted && !node.getCode().equals(previousNode)) {
                lot.setEdgeNodeCode(node.getCode());
                toSave.add(lot);
                affectedCodes.add(node.getCode());
                if (previousNode != null && !previousNode.isBlank()) {
                    // 车场从旧节点转入本节点：旧节点清单变化，也需要重新同步
                    affectedCodes.add(previousNode);
                }
            } else if (!wanted && node.getCode().equals(previousNode)) {
                lot.setEdgeNodeCode(null);
                toSave.add(lot);
                affectedCodes.add(node.getCode());
            }
        }
        if (!toSave.isEmpty()) {
            lots.saveAll(toSave);
        }
        // 绑定关系变化后由 AFTER_COMMIT 监听器对受影响节点补全量/空快照
        if (!affectedCodes.isEmpty()) {
            publishBindingChanged(affectedCodes);
        }
        return EdgeNodeView.from(node, boundLots(node.getCode()));
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        requireSuperAdmin();
        EdgeNode node = requireNode(nodeId);
        // 解绑名下全部车场后再删除节点，避免留下孤儿绑定
        List<ParkingLot> toUnbind = boundLots(node.getCode());
        for (ParkingLot lot : toUnbind) {
            lot.setEdgeNodeCode(null);
        }
        if (!toUnbind.isEmpty()) {
            lots.saveAll(toUnbind);
        }
        nodes.delete(node);
        // 节点已从同步目标摘除：事务提交后向其主题下发空快照，清理边缘本地残留配置
        publishBindingChanged(node.getCode());
    }

    private List<ParkingLot> boundLots(String nodeCode) {
        return lots.findAllByEdgeNodeCodeOrderByCodeAsc(nodeCode);
    }

    /** 发布“节点同步目标关系变化”事件：AFTER_COMMIT 后由 settings 调度器补全量/空快照 */
    private void publishBindingChanged(String nodeCode) {
        publishBindingChanged(List.of(nodeCode));
    }

    private void publishBindingChanged(Collection<String> nodeCodes) {
        EdgeNodeBindingChangedEvent event = EdgeNodeBindingChangedEvent.of(
                nodeCodes == null ? List.of() : nodeCodes.stream().toList());
        if (event != null) {
            eventPublisher.publishEvent(event);
        }
    }

    private EdgeNode requireNode(Long nodeId) {
        return nodes.findById(nodeId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String normalizeRequired(String value, String field) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }

    private void requireSuperAdmin() {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        if (!UserAccount.ROLE_SUPER_ADMIN.equals(operator.getRole())) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
    }
}
