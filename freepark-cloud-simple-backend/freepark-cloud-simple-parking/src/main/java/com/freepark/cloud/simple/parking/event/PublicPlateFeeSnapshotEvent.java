package com.freepark.cloud.simple.parking.event;

import java.util.List;

/**
 * C 端按车牌查费完成后发出：事务提交后异步把刚算过的流水应收快照落库，
 * 不阻塞查费接口响应。
 *
 * @param sessionIds 本次查费命中的流水 ID（在场 + 展示中的已出场未结）
 */
public record PublicPlateFeeSnapshotEvent(List<Long> sessionIds) {
}
