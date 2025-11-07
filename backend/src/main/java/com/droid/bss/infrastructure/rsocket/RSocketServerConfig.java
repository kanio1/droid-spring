package com.droid.bss.infrastructure.rsocket;

import io.rsocket.core.RSocketServer;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.TcpServerTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.messaging.rsocket.annotation.MessageRateLimit;
import org.springframework.messaging.rsocket.service.RSocketService;
import org.springframework.rsocket.RSocketConnector;
import org.springframework.rsocket.core.DefaultResponder;
import org.springframework.rsocket.core.DefaultServerResponder;
import org.springframework.rsocket.handler.ResponseTimeLoggingInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * RSocket Server Configuration
 * Configures the RSocket server with security and rate limiting
 */
@Configuration
public class RSocketServerConfig {

    /**
     * Create RSocket server with TCP transport
     */
    @Bean
    public RSocketServer rsocketServer(ServerTransport<ServerTransport> transport,
                                       DefaultServerResponder responder) {
        RSocketServer server = RSocketServer.create(transport);
        server.responder(responder);
        server.addInterceptors(new ResponseTimeLoggingInterceptor());
        return server;
    }

    /**
     * TCP Server Transport on port 7000
     */
    @Bean
    public TcpServerTransport tcpServerTransport() {
        return TcpServerTransport.create(7000);
    }

    /**
     * Default server responder
     */
    @Bean
    public DefaultServerResponder defaultServerResponder(RSocketService rsocketService) {
        return new DefaultServerResponder(rsocketService);
    }

    /**
     * Message handler for requests
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public NotificationMessageHandler notificationMessageHandler() {
        return new NotificationMessageHandler();
    }

    /**
     * Message Handler for handling notification requests
     */
    public static class NotificationMessageHandler {

        /**
         * Get system status
         */
        @MessageMapping("status")
        public Mono<Map<String, Object>> getStatus() {
            return Mono.just(Map.of(
                "status", "UP",
                "version", "1.0.0",
                "timestamp", System.currentTimeMillis()
            ));
        }

        /**
         * Get server metrics
         */
        @MessageMapping("metrics")
        public Mono<Map<String, Object>> getMetrics() {
            return Mono.just(Map.of(
                "uptime", "00:00:00",
                "memory", Map.of(
                    "used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                    "total", Runtime.getRuntime().totalMemory(),
                    "max", Runtime.getRuntime().maxMemory()
                ),
                "threads", Map.of(
                    "active", Thread.activeCount(),
                    "total", Thread.getAllStackTraces().size()
                )
            ));
        }

        /**
         * Subscribe to notifications
         */
        @MessageMapping("subscribe")
        @MessageRateLimit(limit = 5, per = "10s")
        public Mono<String> subscribe(String notificationType) {
            log.info("Client subscribed to notifications: {}", notificationType);
            return Mono.just("Subscribed to: " + notificationType);
        }

        /**
         * Unsubscribe from notifications
         */
        @MessageMapping("unsubscribe")
        public Mono<String> unsubscribe(String notificationType) {
            log.info("Client unsubscribed from notifications: {}", notificationType);
            return Mono.just("Unsubscribed from: " + notificationType);
        }

        /**
         * Request-reply ping-pong
         */
        @MessageMapping("echo")
        public Mono<String> echo(String message) {
            return Mono.just("Echo: " + message);
        }

        /**
         * Fire-and-forget request
         */
        @MessageMapping("notify")
        @MessageRateLimit(limit = 10, per = "1s")
        public Mono<Void> receiveNotification(Map<String, Object> notification) {
            log.info("Received notification: {}", notification);
            return Mono.empty();
        }

        /**
         * Streaming response - sends periodic updates
         */
        @MessageMapping("stream.updates")
        public Mono<Void> streamUpdates(RSocketRequester requester) {
            return requester.route("stream.data")
                .data(Flux.interval(Duration.ofSeconds(1))
                    .map(i -> Map.of(
                        "timestamp", System.currentTimeMillis(),
                        "value", i,
                        "type", "heartbeat"
                    )))
                .then();
        }

        /**
         * Get current user info
         */
        @MessageMapping("user.info")
        public Mono<Map<String, Object>> getUserInfo() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymous".equals(auth.getName())) {
                return Mono.just(Map.of(
                    "userId", auth.getName(),
                    "authorities", auth.getAuthorities(),
                    "isAuthenticated", auth.isAuthenticated()
                ));
            }
            return Mono.just(Map.of(
                "userId", "anonymous",
                "message", "Not authenticated"
            ));
        }
    }
}
