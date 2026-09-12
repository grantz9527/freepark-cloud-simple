package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepark.cloud.simple.billing.service.BillingSessionChargeService;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.repository.DiscountVehicleRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSessionRepository;
import com.freepark.cloud.simple.settings.entity.EdgeMqttConfig;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeParkingSessionReceiverTest {

    @Mock
    private EdgeMqttConfigService configService;
    @Mock
    private ParkingSessionRepository sessionRepository;
    @Mock
    private ParkingLotRepository lotRepository;
    @Mock
    private DiscountVehicleRepository discountVehicles;
    @Mock
    private ParkingOrderRepository orderRepository;
    @Mock
    private BillingSessionChargeService chargeService;

    private EdgeParkingSessionReceiver receiver;

    @BeforeEach
    void setUp() {
        receiver = new EdgeParkingSessionReceiver(
                configService, sessionRepository, lotRepository, discountVehicles,
                orderRepository, chargeService, new ObjectMapper());
        EdgeMqttConfig config = new EdgeMqttConfig("127.0.0.1", 1883, "test");
        config.setEnabled(true);
        config.setReportSubscribeTopic("parking/report/#");
        when(configService.runtimeConfig()).thenReturn(config);
        ParkingLot lot = new ParkingLot();
        lot.setId(10L);
        lot.setCode("P001");
        lot.setName("测试场");
        lot.setEdgeNodeCode("node-001");
        when(lotRepository.findByCode("P001")).thenReturn(Optional.of(lot));
    }

    @Test
    void staleOpenSnapshotIsDropped() {
        ParkingSession stored = storedOpen(5L, "云A11111");
        when(sessionRepository.findById(1001L)).thenReturn(Optional.of(stored));

        receiver.onMessage("parking/report/node-001", payload(
                "OPEN", 3, "云A11111", null));

        verify(sessionRepository, never()).save(any());
        assertEquals(ParkingSessionStatus.OPEN, stored.getStatus());
    }

    @Test
    void staleClosedSnapshotMergesExitAndKeepsCloudPlate() {
        ParkingSession stored = storedOpen(5L, "云A11111");
        when(sessionRepository.findById(1001L)).thenReturn(Optional.of(stored));
        when(discountVehicles.findByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(10L, "云A11111"))
                .thenReturn(Optional.empty());
        when(chargeService.chargeFor(any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("8.00"));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        receiver.onMessage("parking/report/node-001", payload(
                "CLOSED", 3, "浙B99999", "2026-09-12T08:00:00Z"));

        ArgumentCaptor<ParkingSession> captor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepository).save(captor.capture());
        ParkingSession saved = captor.getValue();
        assertEquals(ParkingSessionStatus.CLOSED, saved.getStatus());
        assertEquals("云A11111", saved.getPlateNumber());
        assertEquals(LocalDateTime.parse("2026-09-12T08:00:00"), saved.getExitTime());
        assertEquals(5L, saved.getCloudRevision());
        assertNotNull(saved.getFeeYuan());
    }

    @Test
    void matchingRevisionClosedSnapshotOverwritesPlate() {
        ParkingSession stored = storedOpen(5L, "云A11111");
        when(sessionRepository.findById(1001L)).thenReturn(Optional.of(stored));
        when(discountVehicles.findByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(10L, "浙B99999"))
                .thenReturn(Optional.empty());
        when(chargeService.chargeFor(any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("8.00"));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        receiver.onMessage("parking/report/node-001", payload(
                "CLOSED", 5, "浙B99999", "2026-09-12T08:00:00Z"));

        ArgumentCaptor<ParkingSession> captor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals("浙B99999", captor.getValue().getPlateNumber());
        assertEquals(ParkingSessionStatus.CLOSED, captor.getValue().getStatus());
    }

    private static ParkingSession storedOpen(long revision, String plate) {
        ParkingSession session = new ParkingSession();
        session.setId(1001L);
        session.setLotId(10L);
        session.setPlateNumber(plate);
        session.setPlateColor(PlateColor.BLUE);
        session.setStatus(ParkingSessionStatus.OPEN);
        session.setEntryTime(LocalDateTime.parse("2026-09-12T01:00:00"));
        session.setCloudRevision(revision);
        session.setEdgeNodeCode("node-001");
        session.setEdgeSessionId("c9a0f7a1-0000-0000-0000-000000000001");
        return session;
    }

    private static byte[] payload(String status, long revision, String plate, String exitTime) {
        String exit = exitTime == null
                ? ""
                : ",\"exitTime\":\"" + exitTime + "\"";
        return ("""
                {"schema":"edge.parking.session/1","edgeCode":"node-001",\
                "sessionId":"c9a0f7a1-0000-0000-0000-000000000001","cloudId":1001,\
                "cloudRevision":%d,"lotCode":"P001","plateNumber":"%s","plateColor":"BLUE",\
                "status":"%s","entryTime":"2026-09-12T01:00:00Z"%s}
                """.formatted(revision, plate, status, exit))
                .getBytes(StandardCharsets.UTF_8);
    }
}
