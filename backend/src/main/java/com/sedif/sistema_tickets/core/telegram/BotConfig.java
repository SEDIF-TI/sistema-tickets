package com.sedif.sistema_tickets.core.telegram;

import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@Slf4j
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