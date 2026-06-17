package ingprompt.patricia.location.application.port.in;

public interface LocationMaintenanceCase {
    void flushLiveToStorage();
    void purgeExpired();
}
