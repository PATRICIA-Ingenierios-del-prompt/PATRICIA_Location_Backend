package ingprompt.patricia.location.infrastructure.backplane;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "backplane")
public class BackplaneProperties {
    private boolean enabled = false;
    private String channel = "patricia:backplane:location";

    private Redis redis = new Redis();

    @Getter
    @Setter
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
    }
}
