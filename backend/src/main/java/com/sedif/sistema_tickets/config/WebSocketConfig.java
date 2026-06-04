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
        // El frontend apuntará a "http://localhost:8080/ws-tickets" para conectarse.
        registry.addEndpoint("/ws-tickets")
                // Permite conexiones desde su frontend en Vite (puerto 5173 u otros)
                .setAllowedOriginPatterns("*") 
                // Habilita el modo de compatibilidad HTTP si el WebSocket nativo falla
                .withSockJS(); 
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 2. CANALES DE SALIDA (DEL SERVIDOR AL FRONTEND)
        // Todo lo que el servidor difunda empezará con "/topic" (ej. "/topic/avisos")
        config.enableSimpleBroker("/topic");
        
        // 3. CANALES DE ENTRADA (DEL FRONTEND AL SERVIDOR)
        // Todo lo que el frontend envíe al backend deberá empezar con "/app"
        config.setApplicationDestinationPrefixes("/app");
    }
}