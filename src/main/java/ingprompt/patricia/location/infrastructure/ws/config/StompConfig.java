package ingprompt.patricia.location.infrastructure.ws.config;

import ingprompt.patricia.location.infrastructure.ws.security.StompSubscriptionAuthInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Spring STOMP/WebSocket broker for the live geo channel.
 *
 * <pre>
 * Client SENDs to:        /app/geo/{eventId}            -> @MessageMapping in the controller
 * Client SUBSCRIBEs to:   /topic/geo/{eventId}          -> public broadcast for an event
 * Private replies on:     /user/queue/geo/{eventId}/... -> session-targeted (e.g. SUBSCRIBE seed)
 * </pre>
 *
 * Auth contract: the API Gateway terminates JWT validation and forwards
 * {@code X-User-Id} on the WebSocket handshake. This config reads it and binds
 * it as the STOMP session Principal — never trusts payload-supplied identity.
 */
@Configuration
@EnableWebSocketMessageBroker
@AllArgsConstructor
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    public static final String USER_ID_ATTR = "userId";

    private final StompSubscriptionAuthInterceptor subscriptionAuth;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/geo")
                .setAllowedOriginPatterns("*")
                // Resolve the Principal from the gateway-injected X-User-Id header.
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {
                        if (req instanceof ServletServerHttpRequest servlet) {
                            String header = servlet.getServletRequest().getHeader("X-User-Id");
                            if (header == null || header.isBlank()) {
                                return false; // reject handshake without identity
                            }
                            try {
                                attrs.put(USER_ID_ATTR, UUID.fromString(header));
                            } catch (IllegalArgumentException ex) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                })
                .setHandshakeHandler(new org.springframework.web.socket.server.support.DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        UUID userId = (UUID) attributes.get(USER_ID_ATTR);
                        // Principal#getName() must be stable per-user; STOMP uses it for /user/... destinations.
                        return () -> userId.toString();
                    }
                });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");   // client SEND  -> @MessageMapping
        registry.enableSimpleBroker("/topic", "/queue");       // server PUBLISH -> subscribers
        registry.setUserDestinationPrefix("/user");            // private per-session ("/user/queue/...")
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Authorize SUBSCRIBE frames: only event participants may join /topic/geo/{eventId}.
        registration.interceptors(subscriptionAuth);
    }
}
