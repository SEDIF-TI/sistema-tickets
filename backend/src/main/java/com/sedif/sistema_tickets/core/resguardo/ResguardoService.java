package com.sedif.sistema_tickets.core.resguardo;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.time.DayOfWeek;

@Service
@RequiredArgsConstructor
public class ResguardoService {

    private final ResguardoRepository resguardoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SedifTelegramBot telegramBot;

    @Transactional
    public ResguardoResponse crearResguardo(ResguardoRequest request, String correoCreador) {
        Usuario usuario = usuarioRepository.findByCorreo(correoCreador)
                .orElseThrow(() -> new IllegalArgumentException("Usuario creador no encontrado"));

        Resguardo resguardo = new Resguardo();
        resguardo.setSolicitanteNombre(request.solicitanteNombre());
        resguardo.setSolicitanteNumero(request.solicitanteNumero()); // <-- Ahora compila perfectamente
        resguardo.setEquipoNombre(request.equipoNombre());
        resguardo.setNumeroSerie(request.numeroSerie());
        resguardo.setAccesorios(request.accesorios());
        resguardo.setEstado(EstadoResguardo.ENTREGADO);
        
        // Mapeo seguro de los nuevos campos
        resguardo.setTelefono(request.telefono());
        resguardo.setDepartamento(request.departamento());
        resguardo.setNumeroInventario(request.numeroInventario());
        resguardo.setCondiciones(request.condiciones());

        // Manejo de la fecha de vencimiento
        if ("Dias".equalsIgnoreCase(request.duracionTipo())) {
            resguardo.setFechaVencimiento(LocalDateTime.now().plusDays(request.duracionCantidad()));
        } else if ("Semanas".equalsIgnoreCase(request.duracionTipo())) {
            resguardo.setFechaVencimiento(LocalDateTime.now().plusWeeks(request.duracionCantidad()));
        } else {
            resguardo.setFechaVencimiento(null); // Permanente
        }

        resguardo.setUsuarioCreador(usuario);
        resguardoRepository.save(resguardo);
        
        return ResguardoResponse.desdeEntidad(resguardo);
    }

    public List<ResguardoResponse> listarTodos() {
        return resguardoRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .map(ResguardoResponse::desdeEntidad)
                .toList();
    }

    @Transactional
    public ResguardoResponse devolverEquipo(Long resguardoId) {
        Resguardo resguardo = resguardoRepository.findById(resguardoId)
                .orElseThrow(() -> new IllegalArgumentException("Resguardo no encontrado"));
        
        resguardo.setEstado(EstadoResguardo.DEVUELTO);
        // Si tienes campo de auditoría para fecha de modificación, se actualizará automáticamente
        return ResguardoResponse.desdeEntidad(resguardoRepository.save(resguardo));
    }

    // ---> CRON JOB: SE EJECUTA TODOS LOS DÍAS A LAS 9:00 AM <---
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void verificarYNotificarVencimientos() {
        System.out.println("[CRON] Iniciando verificación de resguardos vencidos a las 9:00 AM...");
        
        // Buscamos los que están ENTREGADOS y cuya fecha ya se cumplió
        List<Resguardo> vencidos = resguardoRepository.findByEstadoAndFechaVencimientoLessThanEqual(
                EstadoResguardo.ENTREGADO, LocalDateTime.now());

        for (Resguardo r : vencidos) {
            // 1. Cambiamos el estado en BD
            r.setEstado(EstadoResguardo.VENCIDO);
            resguardoRepository.save(r);

            // 2. Preparamos el mensaje
            String mensaje = "⚠️ *RESGUARDO VENCIDO* ⚠️\n\n" +
                             "El préstamo de equipo ha superado su límite de tiempo.\n" +
                             "👤 *Solicitante:* " + r.getSolicitanteNombre() + " (No. " + r.getSolicitanteNumero() + ")\n" +
                             "💻 *Equipo:* " + r.getEquipoNombre() + " - SN: " + r.getNumeroSerie() + "\n" +
                             "Por favor, contacta al usuario para la devolución.";

            // 3. Notificamos por Telegram al creador del resguardo
            if (r.getUsuarioCreador().getTelegramChatId() != null) {
                try {
                    telegramBot.enviarMensaje(r.getUsuarioCreador().getTelegramChatId(), mensaje);
                } catch (Exception e) {
                    System.err.println("Error enviando Telegram de resguardo: " + e.getMessage());
                }
            }

            // 4. Notificamos por WebSocket (Alerta en pantalla)
            try {
                // Emitimos a un canal global de resguardos. En React filtraremos para que solo la vea el creador.
                ResguardoResponse payload = ResguardoResponse.desdeEntidad(r);
                messagingTemplate.convertAndSend("/topic/alertas-resguardos", payload);
            } catch (Exception e) {
                System.err.println("Error enviando WebSocket de resguardo: " + e.getMessage());
            }
        }
        
        if (vencidos.isEmpty()) {
            System.out.println("[CRON] No hay resguardos vencidos hoy.");
        } else {
            System.out.println("[CRON] Se actualizaron y notificaron " + vencidos.size() + " resguardos.");
        }
    }
}