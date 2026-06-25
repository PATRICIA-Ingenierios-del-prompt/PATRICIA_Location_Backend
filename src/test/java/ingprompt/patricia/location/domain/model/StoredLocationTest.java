package ingprompt.patricia.location.domain.model;

import ingprompt.patricia.location.domain.enums.PersistenceReason;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoredLocationTest {

    private LiveLocation live() {
        return new LiveLocation(UUID.randomUUID(), UUID.randomUUID(), new GeoPoint(4.6, -74.0), Instant.now());
    }

    @Test
    void routineFlush_isExpiringAndHasNoReport() {
        StoredLocation routine = StoredLocation.routineFlush(live(), Duration.ofHours(12));

        assertThat(routine.getReason()).isEqualTo(PersistenceReason.ROUTINE_FLUSH);
        assertThat(routine.getExpiresAt()).isNotNull();
        assertThat(routine.getReportId()).isNull();
    }

    @Test
    void incident_isPermanentAndCarriesReportId() {
        UUID reportId = UUID.randomUUID();
        StoredLocation incident = StoredLocation.incident(live(), reportId);

        assertThat(incident.getReason()).isEqualTo(PersistenceReason.INCIDENT_REPORT);
        assertThat(incident.getExpiresAt()).isNull();
        assertThat(incident.getReportId()).isEqualTo(reportId);
    }
}
