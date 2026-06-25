package ingprompt.patricia.location.infrastructure.scheduling;

import ingprompt.patricia.location.application.port.in.LocationMaintenanceCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationMaintenanceSchedulerTest {

    @Mock
    private LocationMaintenanceCase maintenanceCase;
    @InjectMocks
    private LocationMaintenanceScheduler scheduler;

    @Test
    void flush_triggersFlushLiveToStorage() {
        scheduler.flush();
        verify(maintenanceCase).flushLiveToStorage();
    }

    @Test
    void purge_triggersPurgeExpired() {
        scheduler.purge();
        verify(maintenanceCase).purgeExpired();
    }
}
