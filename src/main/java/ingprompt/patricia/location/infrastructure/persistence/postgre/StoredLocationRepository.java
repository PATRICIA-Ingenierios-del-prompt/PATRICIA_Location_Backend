package ingprompt.patricia.location.infrastructure.persistence.postgre;

import ingprompt.patricia.location.infrastructure.persistence.entity.StoredLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredLocationRepository extends JpaRepository<StoredLocationEntity, UUID> {
    List<StoredLocationEntity> findByEventId(UUID eventId);

    @Query("""
            select s from StoredLocationEntity s
             where s.eventId = :eventId
               and s.userId = :userId
               and s.reason = ingprompt.patricia.location.domain.enums.PersistenceReason.ROUTINE_FLUSH
            """)
    Optional<StoredLocationEntity> findRoutineFor(@Param("eventId") UUID eventId, @Param("userId") UUID userId);

    @Modifying
    @Query("delete from StoredLocationEntity s where s.expiresAt is not null and s.expiresAt <= :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
