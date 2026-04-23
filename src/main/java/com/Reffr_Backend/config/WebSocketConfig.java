package com.Reffr_Backend.config;

import com.Reffr_Backend.module.chat.websocket.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for topics the client subscribes to
        registry.enableSimpleBroker(
                "/topic",   // broadcast (e.g. new post notifications)
                "/queue"    // user-specific (e.g. private chat messages)
        );
        // Prefix for messages routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix for user-targeted messages (convertAndSendToUser)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // tighten in prod with actual frontend URL
                .withSockJS();                  // SockJS fallback for browsers without WebSocket
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Validate JWT on every CONNECT frame
        registration.interceptors(stompAuthInterceptor);
    }
}