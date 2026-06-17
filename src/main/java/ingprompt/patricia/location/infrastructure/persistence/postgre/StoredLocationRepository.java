package ingprompt.patricia.location.infrastructure.persistence.postgre;

import ingprompt.patricia.location.infrastructure.persistence.entity.StoredLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StoredLocationRepository extends JpaRepository<StoredLocationEntity, UUID> {
    List<StoredLocationEntity> findByEventId(UUID eventId);

    @Modifying
    @Query("delete from StoredLocationEntity s where s.expiresAt is not null and s.expiresAt <= :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
