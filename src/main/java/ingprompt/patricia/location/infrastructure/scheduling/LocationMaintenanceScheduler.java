package ingprompt.patricia.location.infrastructure.scheduling;

import ingprompt.patricia.location.application.port.in.LocationMaintenanceCase;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LocationMaintenanceScheduler {
    private final LocationMaintenanceCase maintenanceCase;

    @Scheduled(fixedDelayString = "${location.flush.delay-ms}")
    public void flush() {
        maintenanceCase.flushLiveToStorage();
    }

    @Scheduled(fixedDelayString = "${location.purge.delay-ms:3600000}")
    public void purge() {
        maintenanceCase.purgeExpired();
    }
}
