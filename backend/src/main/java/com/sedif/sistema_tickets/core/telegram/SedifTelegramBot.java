package com.sedif.sistema_tickets.core.telegram;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class SedifTelegramBot extends TelegramLongPollingBot {

    private final UsuarioRepository usuarioRepository;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public SedifTelegramBot(@Value("${telegram.bot.token}") String botToken, UsuarioRepository usuarioRepository) {
        super(botToken);
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    @Transactional // <--- Fuerza a que la base de datos abra y cierre la transacción correctamente
    public void onUpdateReceived(Update update) {
        log.debug("Mensaje entrante de Telegram.");

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            // El texto y el chat son datos personales: no se registran.
            log.debug("Procesando comando de Telegram.");

            if (messageText.startsWith("/start")) {
                try {
                    String idString = messageText.replace("/start", "").trim();
                    
                    // Si el usuario abre el bot directo sin pasar por el sistema
                    if (idString.isEmpty()) {
                        enviarMensaje(chatId, "Bienvenido. Por favor, vincula tu cuenta dando clic al botón desde el perfil de tu sistema SEDIF.");
                        return;
                    }

                    Long usuarioId = Long.parseLong(idString);
                    log.debug("Vinculando cuenta de Telegram para el usuario id={}", usuarioId);

                    Usuario tecnico = usuarioRepository.findById(usuarioId).orElse(null);
                    
                    if (tecnico != null) {
                        log.debug("Usuario id={} localizado para vinculacion.", usuarioId);
                        
                        tecnico.setTelegramChatId(chatId);
                        
                        // Forzamos la escritura inmediata en la BD
                        usuarioRepository.saveAndFlush(tecnico); 
                        
                        log.info("Cuenta de Telegram vinculada al usuario id={}", usuarioId);
                        
                        enviarMensaje(chatId, "¡Hola " + tecnico.getNombre() + "! Tu cuenta ha sido vinculada exitosamente al Sistema de Tickets SEDIF. A partir de ahora recibirás tus notificaciones aquí.");
                    } else {
                        log.warn("Intento de vinculacion con un usuario inexistente: id={}", usuarioId);
                        enviarMensaje(chatId, "Error: No se encontró ningún usuario con ese ID en el sistema.");
                    }
                } catch (NumberFormatException e) {
                    log.warn("Intento de vinculacion con un identificador no numerico.");
                } catch (Exception e) {
                    log.error("Error al procesar el mensaje de Telegram.", e);
                }
            }
        }
    }

    public void enviarMensaje(Long chatId, String texto) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(texto);

        message.setParseMode("Markdown"); // Permite usar Markdown en el mensaje
        // Sin try-catch para que TicketService pueda atrapar el error si Telegram falla
        execute(message);
    }
}