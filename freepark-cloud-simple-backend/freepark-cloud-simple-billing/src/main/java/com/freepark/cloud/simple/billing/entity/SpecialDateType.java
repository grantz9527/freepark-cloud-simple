package com.freepark.cloud.simple.billing.entity;

/**
 * 计费特殊日期类型。
 *
 * <p>默认口径：工作日收费、周六日免费；通过以下两类特殊日期覆盖默认判断。</p>
 */
public enum SpecialDateType {

    /** 节假日：当天无论周几均免费（覆盖工作日收费） */
    HOLIDAY,

    /** 补班日：周末/休息日调为上班时间，按工作日计费（覆盖周末免费） */
    MAKEUP_WORKDAY
}
