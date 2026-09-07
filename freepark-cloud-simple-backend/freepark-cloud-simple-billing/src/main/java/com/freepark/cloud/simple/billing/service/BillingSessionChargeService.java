package com.freepark.cloud.simple.billing.service;

import com.freepark.cloud.simple.billing.dto.BillingSimulateRequest;
import com.freepark.cloud.simple.billing.entity.BillingLotBinding;
import com.freepark.cloud.simple.billing.repository.BillingLotBindingRepository;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 按「车场计费绑定」结算一次停车应收金额（写路径专用）。
 * <p>
 * 本服务只负责两件事：
 * <ol>
 *   <li><b>绑定解析</b>：按「车场 × 车牌颜色 × 入场自然日」从当前生效的
 *       {@link BillingLotBinding} 中选出唯一适用的计费模板——专属颜色绑定优先，
 *       其次回退到默认（plateColor 为空）绑定；生效期以入场所在自然日判断（起止均含，空不限）。</li>
 *   <li><b>费用结算</b>：把选中的模板交给 {@link BillingSimulateService} 模拟引擎，
 *       对「入场 → 出场」区间计算应收金额（每日制 / 24 小时制口径均由引擎负责）。</li>
 * </ol>
 * 结算仅在业务事件（新增/出入场更新/手动重新算费）时按需调用并把结果快照到流水上；
 * 列表查询不做实时计算。未命中绑定、模板缺失或区间超出引擎支持范围时返回 {@code null}，
 * 表示该笔流水当前「未计费」，由调用方决定展示。
 * </p>
 */
@Service
public class BillingSessionChargeService {

    private final BillingLotBindingRepository bindings;
    private final BillingSimulateService simulateService;
    private final SiteZoneProvider zoneProvider;

    public BillingSessionChargeService(BillingLotBindingRepository bindings,
                                       BillingSimulateService simulateService,
                                       SiteZoneProvider zoneProvider) {
        this.bindings = bindings;
        this.simulateService = simulateService;
        this.zoneProvider = zoneProvider;
    }

    /**
     * 为一段「入场 → 出场」停车结算应收金额。
     *
     * @param lotId       车场 id
     * @param plateColor  车牌颜色（{@code null} = 未知，只可能命中默认绑定）
     * @param entryAnchor 入场时间（UTC 锚点）
     * @param exitAnchor  出场时间（UTC 锚点）
     * @return 应收金额；未计费 / 不可结算时返回 {@code null}
     */
    @Transactional(readOnly = true)
    public BigDecimal chargeFor(Long lotId, String plateColor,
                                LocalDateTime entryAnchor, LocalDateTime exitAnchor) {
        if (lotId == null || entryAnchor == null || exitAnchor == null
                || !exitAnchor.isAfter(entryAnchor)) {
            return null;
        }
        ZoneId zone = zoneProvider.currentZone();
        // 入场/出场站内本地墙钟（用于按自然日判定绑定与计费窗口）
        LocalDateTime entryWall = SiteZoneTimes.toSiteWall(entryAnchor, zone);
        LocalDateTime exitWall = SiteZoneTimes.toSiteWall(exitAnchor, zone);

        // 命中绑定：其生效区间与「入场自然日 ~ 出场自然日」的停车区间有交集。
        // 允许「入场早于绑定生效日、出场晚于生效日」的流水命中，计费起点收敛到生效日。
        BillingLotBinding binding = resolveBinding(
                bindings.findByLotIdOrderByEffectiveFromAscIdAsc(lotId),
                entryWall.toLocalDate(), exitWall.toLocalDate(), plateColor);
        if (binding == null) {
            return null;
        }

        // 计费起点：入场时刻与绑定生效日零点取较晚者（生效前时段不收费）
        LocalDateTime chargedStartWall = entryWall;
        if (binding.getEffectiveFrom() != null) {
            LocalDateTime fromStartWall = LocalDateTime.of(binding.getEffectiveFrom(), LocalTime.MIN);
            if (chargedStartWall.isBefore(fromStartWall)) {
                chargedStartWall = fromStartWall;
            }
        }
        // 计费终点：出场时刻与绑定失效日次零点取较早者（失效后时段不收费）
        LocalDateTime chargedEndWall = exitWall;
        if (binding.getEffectiveTo() != null) {
            LocalDateTime toEndWall = LocalDateTime.of(
                    binding.getEffectiveTo().plusDays(1), LocalTime.MIN);
            if (chargedEndWall.isAfter(toEndWall)) {
                chargedEndWall = toEndWall;
            }
        }
        if (!chargedEndWall.isAfter(chargedStartWall)) {
            return null;
        }

        BillingSimulateRequest interval = new BillingSimulateRequest(
                SiteZoneTimes.toUtcAnchor(chargedStartWall, zone),
                SiteZoneTimes.toUtcAnchor(chargedEndWall, zone));
        try {
            if (BillingLotBinding.TYPE_DAILY.equals(binding.getRuleType())) {
                return simulateService.simulateDaily(binding.getRuleId(), interval).totalYuan();
            }
            return simulateService.simulateGeneral(binding.getRuleId(), interval).totalYuan();
        } catch (BizException e) {
            // 模板缺失（已删除）或区间超范围等：视为当前不可结算，返回 null
            return null;
        }
    }

    /**
     * 在与停车区间（入场日 ~ 出场日）有交集的绑定中：先精确匹配专属车牌颜色，无则回退默认绑定。
     * 同一车场同一颜色生效区间不可重叠，故有交集时结果唯一；无交集返回 {@code null}。
     */
    private BillingLotBinding resolveBinding(List<BillingLotBinding> all,
                                             LocalDate entryDay, LocalDate exitDay, String color) {
        BillingLotBinding fallback = null;
        for (BillingLotBinding binding : all) {
            if (!covers(binding, entryDay, exitDay)) {
                continue;
            }
            if (binding.getPlateColor() == null) {
                if (fallback == null) {
                    fallback = binding;
                }
            } else if (color != null && binding.getPlateColor().equalsIgnoreCase(color)) {
                return binding;
            }
        }
        return fallback;
    }

    /**
     * 生效区间是否与停车日区间相交：任一端为空 = 不限；起止自然日均含。
     * 即存在某个自然日既被生效区间覆盖、又落在入场日与出场日之间。
     */
    private boolean covers(BillingLotBinding binding, LocalDate entryDay, LocalDate exitDay) {
        if (binding.getEffectiveFrom() != null && exitDay.isBefore(binding.getEffectiveFrom())) {
            return false;
        }
        if (binding.getEffectiveTo() != null && entryDay.isAfter(binding.getEffectiveTo())) {
            return false;
        }
        return true;
    }
}
