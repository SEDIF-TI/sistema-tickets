package com.sedif.sistema_tickets.core.telegram;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
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
        System.out.println("\n--- [TELEGRAM] NUEVO MENSAJE RECIBIDO ---");

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            System.out.println("[TELEGRAM] Texto: '" + messageText + "' | ChatID: " + chatId);

            if (messageText.startsWith("/start")) {
                try {
                    String idString = messageText.replace("/start", "").trim();
                    
                    // Si el usuario abre el bot directo sin pasar por el sistema
                    if (idString.isEmpty()) {
                        enviarMensaje(chatId, "Bienvenido. Por favor, vincula tu cuenta dando clic al botón desde el perfil de tu sistema SEDIF.");
                        return;
                    }

                    Long usuarioId = Long.parseLong(idString);
                    System.out.println("[TELEGRAM] Buscando al usuario con ID: " + usuarioId);

                    Usuario tecnico = usuarioRepository.findById(usuarioId).orElse(null);
                    
                    if (tecnico != null) {
                        System.out.println("[TELEGRAM] ¡Usuario encontrado! Nombre: " + tecnico.getNombre());
                        
                        tecnico.setTelegramChatId(chatId);
                        
                        // Forzamos la escritura inmediata en la BD
                        usuarioRepository.saveAndFlush(tecnico); 
                        
                        System.out.println("[TELEGRAM] ✅ ChatID guardado y forzado en la base de datos.");
                        
                        enviarMensaje(chatId, "¡Hola " + tecnico.getNombre() + "! Tu cuenta ha sido vinculada exitosamente al Sistema de Tickets SEDIF. A partir de ahora recibirás tus notificaciones aquí.");
                    } else {
                        System.out.println("[TELEGRAM] ❌ Error: El usuario " + usuarioId + " no existe.");
                        enviarMensaje(chatId, "Error: No se encontró ningún usuario con ese ID en el sistema.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[TELEGRAM] ❌ Error: El ID proporcionado no es un número válido.");
                } catch (Exception e) {
                    System.out.println("[TELEGRAM] ❌ ERROR GRAVE: " + e.getMessage());
                    e.printStackTrace(); 
                }
            }
        }
    }

    public void enviarMensaje(Long chatId, String texto) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(texto);
        // Sin try-catch para que TicketService pueda atrapar el error si Telegram falla
        execute(message);
    }
}