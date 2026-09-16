package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.InterceptRuleType;
import com.freepark.cloud.simple.parking.entity.ParkingLot;

import java.util.ArrayList;
import java.util.List;

/**
 * 车场入口/出口拦截规则视图。
 */
public record LotInterceptView(List<InterceptRuleType> entryRules, List<InterceptRuleType> exitRules) {

    public static LotInterceptView from(ParkingLot lot) {
        return new LotInterceptView(
                rulesForEntry(lot),
                rulesForExit(lot));
    }

    private static List<InterceptRuleType> rulesForEntry(ParkingLot lot) {
        return buildRules(lot.isEntryInterceptArrears(), lot.isEntryInterceptBlacklist(),
                lot.isEntryInterceptFull());
    }

    private static List<InterceptRuleType> rulesForExit(ParkingLot lot) {
        return buildRules(lot.isExitInterceptArrears(), lot.isExitInterceptBlacklist(), false);
    }

    private static List<InterceptRuleType> buildRules(boolean arrears, boolean blacklist, boolean full) {
        List<InterceptRuleType> rules = new ArrayList<>();
        if (arrears) {
            rules.add(InterceptRuleType.ARREARS);
        }
        if (blacklist) {
            rules.add(InterceptRuleType.BLACKLIST);
        }
        if (full) {
            rules.add(InterceptRuleType.FULL);
        }
        return rules;
    }
}
