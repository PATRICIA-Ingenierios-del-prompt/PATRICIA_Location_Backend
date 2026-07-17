package ingprompt.patricia.location.application.service;

import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.domain.model.StoredLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationTrackingServiceTest {

    private static final long LIVE_TTL_SECONDS = 300;
    private static final long ROUTINE_TTL_HOURS = 12;

    @Mock
    private LiveLocationStoreOutPort liveStore;

    @Mock
    private StoredLocationRepositoryOutPort storedRepository;

    @Captor
    private ArgumentCaptor<List<StoredLocation>> batchCaptor;

    private LocationTrackingService service;

    @BeforeEach
    void setUp() {
        service = new LocationTrackingService(liveStore, storedRepository, LIVE_TTL_SECONDS, ROUTINE_TTL_HOURS);
    }

    @Test
    void updateLiveLocation_savesWithConfiguredTtl() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        service.updateLiveLocation(eventId, userId, 4.6, -74.1);

        ArgumentCaptor<LiveLocation> captor = ArgumentCaptor.forClass(LiveLocation.class);
        verify(liveStore).save(captor.capture(), eq(Duration.ofSeconds(LIVE_TTL_SECONDS)));
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().point()).isEqualTo(new GeoPoint(4.6, -74.1));
    }

    @Test
    void liveSnapshot_delegatesToStore() {
        UUID eventId = UUID.randomUUID();
        List<LiveLocation> expected = List.of(liveLocation(eventId));
        when(liveStore.snapshot(eventId)).thenReturn(expected);

        assertThat(service.liveSnapshot(eventId)).isEqualTo(expected);
    }

    @Test
    void startTracking_registersEvent() {
        UUID eventId = UUID.randomUUID();
        Set<UUID> participants = Set.of(UUID.randomUUID());

        service.startTracking(eventId, participants);

        verify(liveStore).registerEvent(eventId, participants);
    }

    @Test
    void startTracking_handlesNullParticipants() {
        UUID eventId = UUID.randomUUID();

        service.startTracking(eventId, null);

        verify(liveStore).registerEvent(eventId, null);
    }

    @Test
    void stopTracking_flushesSnapshotThenClearsEvent() {
        UUID eventId = UUID.randomUUID();
        when(liveStore.snapshot(eventId)).thenReturn(List.of(liveLocation(eventId)));

        service.stopTracking(eventId);

        verify(storedRepository).saveAll(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).hasSize(1);
        verify(liveStore).clearEvent(eventId);
    }

    @Test
    void stopTracking_withEmptySnapshotStillClearsEvent() {
        UUID eventId = UUID.randomUUID();
        when(liveStore.snapshot(eventId)).thenReturn(List.of());

        service.stopTracking(eventId);

        verify(storedRepository, never()).saveAll(anyList());
        verify(liveStore).clearEvent(eventId);
    }

    @Test
    void flushLiveToStorage_flushesEachActiveEvent() {
        UUID eventA = UUID.randomUUID();
        when(liveStore.activeEventIds()).thenReturn(Set.of(eventA));
        when(liveStore.snapshot(eventA)).thenReturn(List.of(liveLocation(eventA)));

        service.flushLiveToStorage();

        verify(storedRepository).saveAll(anyList());
    }

    @Test
    void purgeExpired_delegatesToRepository() {
        when(storedRepository.deleteExpired(any(Instant.class))).thenReturn(3);

        service.purgeExpired();

        verify(storedRepository).deleteExpired(any(Instant.class));
    }

    @Test
    void purgeExpired_noRowsRemoved_stillCompletes() {
        when(storedRepository.deleteExpired(any(Instant.class))).thenReturn(0);

        service.purgeExpired();

        verify(storedRepository).deleteExpired(any(Instant.class));
    }

    @Test
    void captureIncidentSnapshot_persistsPermanentEvidence() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        when(liveStore.snapshot(eventId)).thenReturn(List.of(liveLocation(eventId)));

        service.captureIncidentSnapshot(eventId, reportId);

        verify(storedRepository).saveAll(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).hasSize(1);
        assertThat(batchCaptor.getValue().get(0).getReportId()).isEqualTo(reportId);
    }

    @Test
    void captureIncidentSnapshot_withNoLivePositions_persistsNothing() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        when(liveStore.snapshot(eventId)).thenReturn(List.of());

        service.captureIncidentSnapshot(eventId, reportId);

        verify(storedRepository, never()).saveAll(anyList());
    }

    private LiveLocation liveLocation(UUID eventId) {
        return new LiveLocation(eventId, UUID.randomUUID(), new GeoPoint(4.6, -74.1), Instant.now());
    }
}
