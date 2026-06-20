package ingprompt.patricia.location.infrastructure.scheduling;

import ingprompt.patricia.location.application.port.in.LocationMaintenanceCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class LocationMaintenanceScheduler {
    private final LocationMaintenanceCase maintenanceCase;

    @Scheduled(fixedDelayString = "${location.flush.delay-ms}")
    public void flush() {
        try {
            maintenanceCase.flushLiveToStorage();
        } catch (Exception e) {
            log.error("Scheduled flush of live locations to storage failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${location.purge.delay-ms:3600000}")
    public void purge() {
        try {
            maintenanceCase.purgeExpired();
        } catch (Exception e) {
            log.error("Scheduled purge of expired locations failed: {}", e.getMessage(), e);
        }
    }
}
