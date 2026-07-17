package ingprompt.patricia.location;

import ingprompt.patricia.location.infrastructure.persistence.postgre.AuditLogRepository;
import ingprompt.patricia.location.infrastructure.persistence.postgre.StoredLocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class LocationApplicationTests {

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	private StoredLocationRepository storedLocationRepository;

	@MockitoBean
	private AuditLogRepository auditLogRepository;

	@Test
	void contextLoads() {
	}

}
