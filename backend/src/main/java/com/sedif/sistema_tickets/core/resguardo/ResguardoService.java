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
        resguardo.setSolicitanteNumero(request.solicitanteNumero());
        resguardo.setEquipoNombre(request.equipoNombre());
        resguardo.setNumeroSerie(request.numeroSerie());
        resguardo.setAccesorios(request.accesorios());
        resguardo.setEstado(EstadoResguardo.ENTREGADO);
        
        resguardo.setTelefono(request.telefono());
        resguardo.setDepartamento(request.departamento());
        resguardo.setNumeroInventario(request.numeroInventario());
        resguardo.setCondiciones(request.condiciones());

        if ("Dias".equalsIgnoreCase(request.duracionTipo())) {
            resguardo.setFechaVencimiento(LocalDateTime.now().plusDays(request.duracionCantidad()));
        } else if ("Semanas".equalsIgnoreCase(request.duracionTipo())) {
            resguardo.setFechaVencimiento(LocalDateTime.now().plusWeeks(request.duracionCantidad()));
        } else {
            resguardo.setFechaVencimiento(null);
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
        return ResguardoResponse.desdeEntidad(resguardoRepository.save(resguardo));
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * *")
    public void verificarVencimientos() {
        LocalDateTime ahora = LocalDateTime.now();
        // 👈 CORREGIDO: Buscamos resguardos con estado ENTREGADO
        List<Resguardo> vencidos = resguardoRepository.findByFechaVencimientoBeforeAndEstado(ahora, EstadoResguardo.ENTREGADO);

        if (vencidos.isEmpty()) {
            System.out.println("[CRON] No hay resguardos vencidos hoy.");
            return;
        }

        for (Resguardo r : vencidos) {
            try {
                r.setEstado(EstadoResguardo.VENCIDO);
                resguardoRepository.save(r);

                if (r.getUsuarioCreador() != null && r.getUsuarioCreador().getTelegramChatId() != null) {
                    try {
                        String solicitante = r.getSolicitanteNombre() != null ? r.getSolicitanteNombre() : "SIN NOMBRE";
                        String numero = r.getSolicitanteNumero() != null ? r.getSolicitanteNumero() : "N/A";
                        String equipo = r.getEquipoNombre() != null ? r.getEquipoNombre() : "EQUIPO";
                        String serie = r.getNumeroSerie() != null ? r.getNumeroSerie() : "S/N";

                        String mensaje = "⚠️ ALERTA DE RESGUARDO VENCIDO ⚠️\n\n" +
                                        "El resguardo de " + solicitante + " ha vencido hoy.\n" +
                                        "👤 Solicitante: " + solicitante + " (No. " + numero + ")\n" +
                                        "💻 Equipo: " + equipo + " - SN: " + serie + "\n" +
                                        "Por favor, contacta al usuario para la devolución.";

                        telegramBot.enviarMensaje(r.getUsuarioCreador().getTelegramChatId(), mensaje);
                    } catch (Exception e) {
                        System.err.println("[TELEGRAM ERROR] No se pudo enviar mensaje del resguardo ID " + r.getId() + ": " + e.getMessage());
                    }
                }

                try {
                    ResguardoResponse payload = ResguardoResponse.desdeEntidad(r);
                    messagingTemplate.convertAndSend("/topic/alertas-resguardos", payload);
                } catch (Exception e) {
                    System.err.println("[WEBSOCKET ERROR] No se pudo emitir alerta del resguardo ID " + r.getId() + ": " + e.getMessage());
                }

            } catch (Exception e) {
                System.err.println("[CRON ERROR] Error procesando el resguardo ID " + r.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("[CRON] Se actualizaron y notificaron " + vencidos.size() + " resguardos vencidos.");
    }
}