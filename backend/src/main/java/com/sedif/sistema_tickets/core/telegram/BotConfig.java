package com.sedif.sistema_tickets.core.telegram;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(SedifTelegramBot sedifTelegramBot) {
        try {
            // Inicializamos la API de Telegram con la sesión por defecto
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            
            // Registramos tu bot para que empiece a "escuchar" (Long Polling)
            api.registerBot(sedifTelegramBot);
            
            System.out.println("[SISTEMA] Bot de Telegram registrado y escuchando correctamente.");
            return api;
            
        } catch (TelegramApiException e) {
            System.err.println("[SISTEMA] Error crítico al iniciar el bot de Telegram: " + e.getMessage());
            return null;
        }
    }
}