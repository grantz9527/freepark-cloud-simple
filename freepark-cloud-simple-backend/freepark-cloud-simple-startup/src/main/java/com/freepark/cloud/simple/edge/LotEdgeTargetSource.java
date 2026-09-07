package com.freepark.cloud.simple.edge;

import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeSyncTarget;
import com.freepark.cloud.simple.settings.runtime.EdgeSyncTargetSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 车场作为边缘配置下发目标：仅返回“启用”且编码可作为 MQTT 主题段
 * （无空白、无通配符 #/+、不含斜杠、长度合规）的车场。
 */
@Component
public class LotEdgeTargetSource implements EdgeSyncTargetSource {

    private static final int MAX_CODE_LENGTH = 64;

    private final ParkingLotRepository lotRepository;

    public LotEdgeTargetSource(ParkingLotRepository lotRepository) {
        this.lotRepository = lotRepository;
    }

    @Override
    public List<EdgeSyncTarget> targets() {
        return lotRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(ParkingLot::isEnabled)
                .map(ParkingLot::getCode)
                .filter(LotEdgeTargetSource::isTopicSafeCode)
                .map(EdgeSyncTarget::new)
                .toList();
    }

    /** 编码将作为主题段拼接进 “{前缀}/{编码}”，需满足主题安全约束 */
    private static boolean isTopicSafeCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (code.length() > MAX_CODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '/' || c == '#' || c == '+' || c == '\u0000' || Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
}
