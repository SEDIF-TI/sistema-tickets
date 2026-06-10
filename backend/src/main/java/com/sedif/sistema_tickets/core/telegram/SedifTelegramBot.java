package com.sedif.sistema_tickets.core.telegram;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class SedifTelegramBot extends TelegramLongPollingBot {

    private final UsuarioRepository usuarioRepository;

    @Value("${telegram.bot.username}")
    private String botUsername;

    // El constructor inyecta el token desde el application.properties y el repositorio
    public SedifTelegramBot(@Value("${telegram.bot.token}") String botToken, UsuarioRepository usuarioRepository) {
        super(botToken);
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    // Este método se dispara automáticamente cada vez que alguien le escribe al bot
    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("[TELEGRAM] ¡Alerta! Alguien interactuó con el bot.");

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            System.out.println("[TELEGRAM] Texto recibido: '" + messageText + "' desde el ChatID: " + chatId);

            if (messageText.startsWith("/start ")) {
                System.out.println("⚙️ [TELEGRAM] Detectado comando de vinculación...");
                try {
                    String idString = messageText.replace("/start ", "").trim();
                    Long usuarioId = Long.parseLong(idString);
                    System.out.println("🔍 [TELEGRAM] Buscando en BD al usuario con ID: " + usuarioId);

                    Usuario tecnico = usuarioRepository.findById(usuarioId).orElse(null);
                    
                    if (tecnico != null) {
                        System.out.println("[TELEGRAM] ¡Usuario encontrado! Nombre: " + tecnico.getNombre());
                        tecnico.setTelegramChatId(chatId);
                        usuarioRepository.save(tecnico);
                        System.out.println("[TELEGRAM] ChatID guardado en la base de datos.");
                        
                        enviarMensaje(chatId, "¡Hola " + tecnico.getNombre() + "! Tu cuenta ha sido vinculada exitosamente al Sistema de Tickets SEDIF.");
                    } else {
                        System.out.println("[TELEGRAM] Error: El usuario " + usuarioId + " no existe en la base de datos.");
                        enviarMensaje(chatId, "Error: No se encontró ningún usuario con ese ID en el sistema.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[TELEGRAM] Error: El ID proporcionado no es un número válido.");
                } catch (Exception e) {
                    System.out.println("[TELEGRAM] ERROR GRAVE al intentar vincular: " + e.getMessage());
                    e.printStackTrace(); // Esto nos imprimirá el error real en rojo
                }
            } else {
                System.out.println("[TELEGRAM] El mensaje no era un comando /start válido.");
            }
        }
    }

    // Método de utilidad para que el bot responda
    public void enviarMensaje(Long chatId, String texto) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(texto);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error al enviar mensaje de Telegram: " + e.getMessage());
        }
    }
}