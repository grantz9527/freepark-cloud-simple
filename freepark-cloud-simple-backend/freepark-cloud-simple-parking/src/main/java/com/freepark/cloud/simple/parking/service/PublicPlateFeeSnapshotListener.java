package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.parking.event.PublicPlateFeeSnapshotEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * C 端查费后异步回写流水应收快照：等查费只读事务提交后再执行，避免拖慢接口。
 */
@Component
public class PublicPlateFeeSnapshotListener {

    private static final Logger log = LoggerFactory.getLogger(PublicPlateFeeSnapshotListener.class);

    private final ParkingSessionService parkingSessionService;

    public PublicPlateFeeSnapshotListener(ParkingSessionService parkingSessionService) {
        this.parkingSessionService = parkingSessionService;
    }

    @Async("parkingFeeSnapshotExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublicPlateFeeQueried(PublicPlateFeeSnapshotEvent event) {
        if (event == null || event.sessionIds() == null || event.sessionIds().isEmpty()) {
            return;
        }
        try {
            parkingSessionService.refreshFeeSnapshotsAfterPublicQuery(event.sessionIds());
        } catch (Exception e) {
            log.warn("C 端查费后异步刷新应收快照失败：{}", e.getMessage());
        }
    }
}
