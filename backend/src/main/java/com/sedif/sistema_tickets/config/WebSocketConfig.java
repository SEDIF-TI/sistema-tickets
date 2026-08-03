package com.sedif.sistema_tickets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Canal de tiempo real que alimenta el panel de soporte.
 *
 * <p>El origen permitido estaba escrito a mano como
 * {@code http://localhost:5173}, asi que el handshake solo funcionaba en el
 * equipo del desarrollador: en cualquier despliegue real el navegador
 * rechazaba la conexion y el panel dejaba de recibir avisos sin mostrar error
 * alguno. Ahora se reutiliza {@code app.cors.allowed-origins}, la misma
 * propiedad que usa {@link WebConfig} para la API REST, de modo que ambos
 * canales no puedan quedar desincronizados.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> origenesPermitidos;

    public WebSocketConfig(@Value("${app.cors.allowed-origins}") String origenes) {
        this.origenesPermitidos = Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Punto de entrada del handshake. Se usa la lista explicita de origenes
        // y no el comodin "*": con SockJS el navegador envia credenciales, y un
        // comodin permitiria a cualquier sitio abrir el canal aprovechando la
        // sesion activa del usuario.
        registry.addEndpoint("/ws-tickets")
                .setAllowedOrigins(origenesPermitidos.toArray(String[]::new))
                // Respaldo por HTTP cuando el WebSocket nativo esta bloqueado
                // por un proxy corporativo.
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Canales de salida (del servidor al frontend).
        config.enableSimpleBroker("/topic");

        // Canales de entrada (del frontend al servidor).
        config.setApplicationDestinationPrefixes("/app");
    }
}
