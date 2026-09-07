package com.freepark.cloud.simple.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 24 小时制计费规则的「周计划」明细行（周一~周日逐天配置，结构同每日制的 billing_daily_slot）。
 * <p>
 * 某一天可有三种状态，本表只保存「需要显式表达」的行，未出现的星期按默认处理：
 * <ul>
 *   <li>当天免费：{@code allDayFree=true}，{@code startMinute/endMinute} 忽略，表示整天不收费；</li>
 *   <li>指定收费时段：{@code allDayFree=false}，{@code [startMinute, endMinute)} 为当天一个收费时段，
 *       同一天可配置多行（多个时段），仅在这些时段内计费；</li>
 *   <li>默认（表中无该星期的行）：当天按规则费率全天收费。</li>
 * </ul>
 * 时间为当天 0 点起的分钟数（0..1440，endMinute 为结束边界，1440 表示次日 0 点 / 24:00）。
 * 同一规则同一天内：收费时段不允许相互重叠；「当天免费」不能与收费时段并存。
 * </p>
 * <p>
 * 算费组合口径（供云端算费引擎）：某收费时刻先按所在自然日做整日判定——命中「节假日免费」
 * （holidayFree 且当天属于全局特殊日期）或「周六日免费」（weekendFree 且当天为周六/周日）时整天不收费；
 * 未命中的自然日再由本表周计划逐天细化（未配置 = 全天可计费；配置 = 仅收费时段内计费 / 当天免费）。
 * 免费时段内不计时，也不消耗计费档位。行归属于某条 24 小时制规则（{@code ruleId} 逻辑关联
 * billing_general_rule.id），更新规则时整组替换，因此不含时间戳。
 * </p>
 */
@Entity
@Table(name = "billing_general_slot")
public class BillingGeneralSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属 24 小时制规则 id（逻辑关联，不建物理外键） */
    @Column(nullable = false)
    private Long ruleId;

    /** 星期：1=周一 ... 7=周日 */
    @Column(nullable = false)
    private int weekday;

    /** 当天是否整体免费（免费日；为 true 时忽略 start/end 时段） */
    @Column(nullable = false)
    private boolean allDayFree;

    /** 收费时段开始（当天 0 点起分钟，0..1439）；免费行时为空 */
    private Integer startMinute;

    /** 收费时段结束边界（当天 0 点起分钟，1..1440，1440 = 24:00）；免费行时为空 */
    private Integer endMinute;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public boolean isAllDayFree() {
        return allDayFree;
    }

    public void setAllDayFree(boolean allDayFree) {
        this.allDayFree = allDayFree;
    }

    public Integer getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(Integer startMinute) {
        this.startMinute = startMinute;
    }

    public Integer getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(Integer endMinute) {
        this.endMinute = endMinute;
    }
}
