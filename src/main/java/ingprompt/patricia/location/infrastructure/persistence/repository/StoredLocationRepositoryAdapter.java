package ingprompt.patricia.location.infrastructure.persistence.repository;

import ingprompt.patricia.location.application.port.out.EncryptionPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.StoredLocation;
import ingprompt.patricia.location.infrastructure.persistence.entity.StoredLocationEntity;
import ingprompt.patricia.location.infrastructure.persistence.postgre.StoredLocationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class StoredLocationRepositoryAdapter implements StoredLocationRepositoryOutPort {
    private final StoredLocationRepository repository;
    private final EncryptionPort encryption;

    @Override
    public void save(StoredLocation location) {
        repository.save(toEncryptedEntity(location));
    }

    @Override
    public void saveAll(List<StoredLocation> locations) {
        repository.saveAll(locations.stream().map(this::toEncryptedEntity).toList());
    }

    @Override
    @Transactional
    public void upsertRoutineLastKnown(StoredLocation location) {
        repository.findRoutineFor(location.getEventId(), location.getUserId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setLatitudeCipher(encryption.encrypt(Double.toString(location.getPoint().latitude())));
                            existing.setLongitudeCipher(encryption.encrypt(Double.toString(location.getPoint().longitude())));
                            existing.setRecordedAt(location.getRecordedAt());
                            existing.setExpiresAt(location.getExpiresAt());
                            // reportId stays null; reason stays ROUTINE_FLUSH.
                            repository.save(existing);
                        },
                        () -> repository.save(toEncryptedEntity(location))
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredLocation> findDecryptedByEventId(UUID eventId) {
        return repository.findByEventId(eventId).stream().map(this::toDecryptedDomain).toList();
    }

    @Override
    @Transactional
    public int deleteExpired(Instant now) {
        return repository.deleteByExpiresAtBefore(now);
    }

    private StoredLocationEntity toEncryptedEntity(StoredLocation location) {
        StoredLocationEntity entity = new StoredLocationEntity();
        entity.setId(location.getId());
        entity.setEventId(location.getEventId());
        entity.setUserId(location.getUserId());
        // Coordinates leave the application as ciphertext and never as plain columns.
        entity.setLatitudeCipher(encryption.encrypt(Double.toString(location.getPoint().latitude())));
        entity.setLongitudeCipher(encryption.encrypt(Double.toString(location.getPoint().longitude())));
        entity.setRecordedAt(location.getRecordedAt());
        entity.setExpiresAt(location.getExpiresAt());
        entity.setReason(location.getReason());
        entity.setReportId(location.getReportId());
        return entity;
    }

    private StoredLocation toDecryptedDomain(StoredLocationEntity entity) {
        double lat = Double.parseDouble(encryption.decrypt(entity.getLatitudeCipher()));
        double lng = Double.parseDouble(encryption.decrypt(entity.getLongitudeCipher()));
        return StoredLocation.rehydrate(entity.getId(), entity.getEventId(), entity.getUserId(), new GeoPoint(lat, lng), entity.getRecordedAt(), entity.getExpiresAt(), entity.getReason(), entity.getReportId());
    }
}
