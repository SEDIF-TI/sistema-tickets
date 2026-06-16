package com.sedif.sistema_tickets.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. PUNTO DE ENTRADA (HANDSHAKE)
        registry.addEndpoint("/ws-tickets")
                // CAMBIO AQUÍ: Usar el origen exacto de React para evitar el bloqueo de CORS/SockJS
                .setAllowedOrigins("http://localhost:5173") 
                // Habilita el modo de compatibilidad HTTP si el WebSocket nativo falla
                .withSockJS(); 
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 2. CANALES DE SALIDA (DEL SERVIDOR AL FRONTEND)
        config.enableSimpleBroker("/topic");
        
        // 3. CANALES DE ENTRADA (DEL FRONTEND AL SERVIDOR)
        config.setApplicationDestinationPrefixes("/app");
    }
}