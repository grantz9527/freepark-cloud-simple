package com.freepark.cloud.simple.billing.entity;

/**
 * 计费周期方案中「停车时长不足当前分段」时的尾数计费方式。
 *
 * <p>计费时按顺序消耗方案内各分段：整段时长消耗完毕后按该段单价收费；
 * 最后一个未满的尾数分段按本枚举决定如何计价。</p>
 */
public enum CycleTailMode {

    /** 尾数不足一段时按整段收费（进一法） */
    WHOLE_SEGMENT,

    /** 尾数按分钟比例折算（未满时长 / 分段时长 × 分段单价） */
    PROPORTIONAL,

    /** 尾数不足一段时不产生该段费用 */
    NO_CHARGE
}
