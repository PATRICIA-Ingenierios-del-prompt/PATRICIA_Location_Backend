package ingprompt.patricia.location.infrastructure.redis;

import ingprompt.patricia.location.application.port.out.EncryptionPort;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.infrastructure.crypto.AesGcmEncryptionAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the Redis live-location store against a real Redis via
 * Testcontainers, using the real AES-GCM encryption adapter (positions are
 * encrypted at the Redis boundary). Requires Docker — executed by Failsafe in CI.
 */
@Testcontainers
class LiveLocationRedisAdapterIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private LiveLocationRedisAdapter adapter;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeAll
    static void startConnection() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void stopConnection() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        template.getConnectionFactory().getConnection().serverCommands().flushAll();

        EncryptionPort encryption = new AesGcmEncryptionAdapter("test-secret");
        ReflectionTestUtils.invokeMethod(encryption, "init");
        adapter = new LiveLocationRedisAdapter(template, encryption);
    }

    private LiveLocation live(double lat, double lng) {
        return new LiveLocation(eventId, userId, new GeoPoint(lat, lng), Instant.now());
    }

    @Test
    void saveThenFindLive_roundTripsDecryptedPosition() {
        adapter.save(live(4.65, -74.05), Duration.ofMinutes(5));

        Optional<LiveLocation> found = adapter.findLive(eventId, userId);

        assertThat(found).isPresent();
        assertThat(found.get().point().latitude()).isEqualTo(4.65);
        assertThat(found.get().point().longitude()).isEqualTo(-74.05);
    }

    @Test
    void snapshot_returnsAllTrackedUsers() {
        adapter.save(live(4.65, -74.05), Duration.ofMinutes(5));

        assertThat(adapter.snapshot(eventId)).hasSize(1);
        assertThat(adapter.activeEventIds()).contains(eventId);
    }

    @Test
    void registerEvent_controlsRosterMembership() {
        assertThat(adapter.isRegistered(eventId, userId)).isFalse();

        adapter.registerEvent(eventId, Set.of(userId));

        assertThat(adapter.isRegistered(eventId, userId)).isTrue();
    }

    @Test
    void clearEvent_removesLiveData() {
        adapter.save(live(4.65, -74.05), Duration.ofMinutes(5));

        adapter.clearEvent(eventId);

        assertThat(adapter.findLive(eventId, userId)).isEmpty();
        assertThat(adapter.snapshot(eventId)).isEmpty();
    }
}
