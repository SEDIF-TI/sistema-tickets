package com.sedif.sistema_tickets.core.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Registro del bot de Telegram.
 *
 * <p>El bean solo se crea si {@code telegram.bot.enabled} es {@code true}. La
 * version anterior registraba el bot siempre, ignorando esa propiedad: al
 * levantar el proyecto en un equipo de desarrollo, el bot se conectaba con el
 * token de produccion y competia con la instancia real por los mensajes
 * entrantes, que Telegram entrega a un solo consumidor. El sintoma era un
 * "[409] Conflict: terminated by other getUpdates request" repetido cada pocos
 * segundos en ambos entornos, y notificaciones perdidas en el de verdad.</p>
 *
 * <p>{@code matchIfMissing = false}: si la propiedad no esta definida, el bot
 * queda desactivado. Es la opcion prudente, porque activarlo por descuido
 * afecta al entorno de produccion, no al local.</p>
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = false)
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(SedifTelegramBot sedifTelegramBot) {
        try {
            // Inicializamos la API de Telegram con la sesión por defecto
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            
            // Registramos tu bot para que empiece a "escuchar" (Long Polling)
            api.registerBot(sedifTelegramBot);
            
            log.info("Bot de Telegram registrado correctamente.");
            return api;
            
        } catch (TelegramApiException e) {
            log.error("No se pudo iniciar el bot de Telegram. Las notificaciones quedaran desactivadas.", e);
            return null;
        }
    }
}