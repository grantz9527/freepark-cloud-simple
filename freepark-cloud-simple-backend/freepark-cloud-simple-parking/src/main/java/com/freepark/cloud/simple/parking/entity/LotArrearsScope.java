package com.freepark.cloud.simple.parking.entity;

/**
 * 车场欠费统计范围：决定「算费请求」按什么维度统计该车牌的欠费金额。
 * <ul>
 *   <li>{@code LOT}：仅统计本车场的欠费（默认），其它车场的欠费不并入；</li>
 *   <li>{@code GLOBAL}：跨全部车场统计，任一车场的未结清欠费都会计入本车场的拦截金额。</li>
 * </ul>
 */
public enum LotArrearsScope {
    LOT,
    GLOBAL
}
