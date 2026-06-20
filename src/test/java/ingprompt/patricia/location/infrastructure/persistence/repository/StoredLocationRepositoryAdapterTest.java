package ingprompt.patricia.location.infrastructure.persistence.repository;

import ingprompt.patricia.location.application.port.out.EncryptionPort;
import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.StoredLocation;
import ingprompt.patricia.location.infrastructure.persistence.entity.StoredLocationEntity;
import ingprompt.patricia.location.infrastructure.persistence.postgre.StoredLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoredLocationRepositoryAdapterTest {

    @Mock
    private StoredLocationRepository repository;

    @Mock
    private EncryptionPort encryption;

    @Captor
    private ArgumentCaptor<StoredLocationEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<List<StoredLocationEntity>> entityListCaptor;

    private StoredLocationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StoredLocationRepositoryAdapter(repository, encryption);
    }

    @Test
    void saveEncryptsCoordinates() {
        UUID id = UUID.randomUUID();
        StoredLocation location = StoredLocation.rehydrate(
                id, UUID.randomUUID(), UUID.randomUUID(),
                new GeoPoint(4.6097, -74.0817), Instant.now(), null,
                PersistenceReason.INCIDENT_REPORT, UUID.randomUUID());

        when(encryption.encrypt("4.6097")).thenReturn("ENC_LAT");
        when(encryption.encrypt("-74.0817")).thenReturn("ENC_LNG");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.save(location);

        verify(repository).save(entityCaptor.capture());
        StoredLocationEntity entity = entityCaptor.getValue();
        assertEquals("ENC_LAT", entity.getLatitudeCipher());
        assertEquals("ENC_LNG", entity.getLongitudeCipher());
        assertEquals(id, entity.getId());
        assertEquals(location.getEventId(), entity.getEventId());
        assertEquals(location.getUserId(), entity.getUserId());
        assertEquals(PersistenceReason.INCIDENT_REPORT, entity.getReason());
    }

    @Test
    void saveAllEncryptsAllLocations() {
        StoredLocation loc1 = StoredLocation.rehydrate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new GeoPoint(1.0, 2.0), Instant.now(), null,
                PersistenceReason.ROUTINE_FLUSH, null);
        StoredLocation loc2 = StoredLocation.rehydrate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new GeoPoint(3.0, 4.0), Instant.now(), null,
                PersistenceReason.INCIDENT_REPORT, UUID.randomUUID());

        when(encryption.encrypt(anyString())).thenAnswer(inv -> "ENC_" + inv.getArgument(0));
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.saveAll(List.of(loc1, loc2));

        verify(repository).saveAll(entityListCaptor.capture());
        List<StoredLocationEntity> entities = entityListCaptor.getValue();
        assertEquals(2, entities.size());
        assertEquals("ENC_1.0", entities.get(0).getLatitudeCipher());
        assertEquals("ENC_2.0", entities.get(0).getLongitudeCipher());
    }

    @Test
    void findDecryptedByEventIdDecryptsAndMaps() {
        UUID eventId = UUID.randomUUID();
        StoredLocationEntity entity = new StoredLocationEntity();
        entity.setId(UUID.randomUUID());
        entity.setEventId(eventId);
        entity.setUserId(UUID.randomUUID());
        entity.setLatitudeCipher("ENC_LAT");
        entity.setLongitudeCipher("ENC_LNG");
        entity.setRecordedAt(Instant.now());
        entity.setExpiresAt(null);
        entity.setReason(PersistenceReason.INCIDENT_REPORT);
        entity.setReportId(UUID.randomUUID());

        when(repository.findByEventId(eventId)).thenReturn(List.of(entity));
        when(encryption.decrypt("ENC_LAT")).thenReturn("4.6097");
        when(encryption.decrypt("ENC_LNG")).thenReturn("-74.0817");

        List<StoredLocation> result = adapter.findDecryptedByEventId(eventId);

        assertEquals(1, result.size());
        StoredLocation loc = result.get(0);
        assertEquals(entity.getId(), loc.getId());
        assertEquals(eventId, loc.getEventId());
        assertEquals(4.6097, loc.getPoint().latitude());
        assertEquals(-74.0817, loc.getPoint().longitude());
        assertEquals(PersistenceReason.INCIDENT_REPORT, loc.getReason());
    }

    @Test
    void deleteExpiredDelegatesToRepository() {
        Instant now = Instant.now();
        when(repository.deleteByExpiresAtBefore(now)).thenReturn(3);

        int result = adapter.deleteExpired(now);

        assertEquals(3, result);
        verify(repository).deleteByExpiresAtBefore(now);
    }
}
