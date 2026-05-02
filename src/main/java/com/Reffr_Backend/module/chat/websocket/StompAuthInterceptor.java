package com.Reffr_Backend.module.chat.websocket;

import com.Reffr_Backend.common.constants.TokenType;
import com.Reffr_Backend.module.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token == null || !jwtProvider.validateToken(token)) {
                log.warn("WebSocket CONNECT rejected - invalid or missing JWT");
                throw new IllegalArgumentException("Invalid JWT token");
            }

            if (jwtProvider.getTokenType(token) == TokenType.REFRESH) {
                log.warn("WebSocket CONNECT rejected - refresh token used");
                throw new IllegalArgumentException("Refresh tokens cannot be used for WebSocket");
            }

            UUID userId = jwtProvider.getUserIdFromToken(token);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            accessor.setUser(auth);
            log.debug("WebSocket CONNECT authenticated - userId={}", userId);
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
