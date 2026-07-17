package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.estatusticket.Estatus;
import com.sedif.sistema_tickets.core.estatusticket.EstatusRepository;
import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import com.sedif.sistema_tickets.core.ticket.bitacora.Bitacora;
import com.sedif.sistema_tickets.core.ticket.bitacora.BitacoraRepository;
import com.sedif.sistema_tickets.core.ticket.filtros.TicketFiltroStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// ---> IMPORTACIÓN NECESARIA PARA WEBSOCKETS
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final SedifTelegramBot telegramBot;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstatusRepository estatusRepository;
    private final Map<String, TicketFiltroStrategy> estrategiasFiltro;
    private final BitacoraRepository bitacoraRepository;
    
    // ---> INYECTAMOS LA HERRAMIENTA DE EMISIÓN DE WEBSOCKETS
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Ticket crearTicket(TicketRequestRecord request, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Estatus estatusAbierto = estatusRepository.findByNombre("ABIERTO")
                .orElseThrow(() -> new IllegalStateException("Estatus ABIERTO no configurado en la base de datos"));

        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(request.titulo());
        nuevoTicket.setDescripcion(request.descripcion());
        nuevoTicket.setSede(request.sede());
        
        // Guardamos el nombre de quien reporta / tiene el resguardo
        if (request.solicitante() != null && !request.solicitante().isBlank()) {
            nuevoTicket.setSolicitanteNombre(request.solicitante());
        }

        nuevoTicket.setUsuarioArea(usuario); 
        nuevoTicket.setEstatus(estatusAbierto); 
        nuevoTicket.setFechaCreacion(LocalDateTime.now());
        nuevoTicket.setCreadoPor(usuario.getCorreo());
        
        String prioridad = (request.prioridad() != null && !request.prioridad().isBlank()) 
                           ? request.prioridad() 
                           : "NORMAL"; 
        nuevoTicket.setPrioridad(prioridad);

        // ---> NUEVA LÓGICA DE ASIGNACIÓN <---
        if (request.usuarioSoporteId() != null) {
            // 1. Asignación directa (Manual por Administrador)
            Usuario soporteElegido = usuarioRepository.findById(request.usuarioSoporteId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario de soporte no encontrado"));
            nuevoTicket.setUsuarioSoporte(soporteElegido);
            System.out.println("DEBUG: Ticket asignado manualmente por el Administrador a: " + soporteElegido.getNombre());
            
        } else if (usuario.getRol() != null && "SOPORTE".equals(usuario.getRol().getNombre())) {
            // 2. Auto-asignación (Si un soporte crea el ticket)
            nuevoTicket.setUsuarioSoporte(usuario);
            System.out.println("DEBUG: Ticket auto-asignado al técnico creador: " + usuario.getNombre());
            
        } else {
            // 3. Balanceador automático (Si un usuario normal lo crea)
            Usuario soporteAsignado = resolverAsignacion(usuario);
            if (soporteAsignado != null) {
                nuevoTicket.setUsuarioSoporte(soporteAsignado);
                System.out.println("DEBUG: Ticket #" + nuevoTicket.getId() + " asignado por balanceador a: " + soporteAsignado.getNombre());
            } else {
                System.err.println("WARNING: Ticket creado sin técnico asignado.");
            }
        }

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        // Notificación automática por Telegram
        if (ticketGuardado.getUsuarioSoporte() != null && 
            ticketGuardado.getUsuarioSoporte().getTelegramChatId() != null) {
            
            // Extraemos el nombre de quien reporta (Dependiendo de si tu front lo manda en "sede" o "solicitanteNombre")
            String afectado = ticketGuardado.getSede(); 
            if (afectado == null || afectado.isBlank()) {
                afectado = ticketGuardado.getUsuarioArea().getNombre(); // Fallback al usuario de la sesión
            }

            // Armamos la plantilla completa con formato Markdown
            String mensaje = "🚨 *NUEVO TICKET ASIGNADO* 🚨\n\n" +
                             "🆔 *Folio:* #" + ticketGuardado.getId() + "\n" +
                             "👤 *Usuario afectado:* " + afectado + "\n" +
                             "📌 *Falla Principal:* " + ticketGuardado.getTitulo() + "\n\n" +
                             "📝 *Descripción detallada:*\n_" + ticketGuardado.getDescripcion() + "_";

            try {
                telegramBot.enviarMensaje(ticketGuardado.getUsuarioSoporte().getTelegramChatId(), mensaje);
                System.out.println("[SISTEMA] Notificación de Telegram enviada con éxito.");
            } catch (Exception e) {
                System.err.println("[SISTEMA] Error al enviar notificación a Telegram: " + e.getMessage());
            }
        } else {
            System.out.println("[SISTEMA] Telegram ignorado: El técnico asignado no tiene un ChatID vinculado.");
        }

        // ---> NUEVO: EMITIR EL EVENTO WEBSOCKET HACIA REACT
        try {
            TicketResponse responsePayload = mapearATicketResponse(ticketGuardado);
            messagingTemplate.convertAndSend("/topic/tickets-soporte", responsePayload);
            System.out.println("✅ [SISTEMA] WebSocket emitido al canal /topic/tickets-soporte");
        } catch (Exception e) {
            System.err.println("❌ [SISTEMA] Error al emitir por WebSocket: " + e.getMessage());
        }

        return ticketGuardado;
    }

    private Usuario resolverAsignacion(Usuario usuarioArea) {
        if (usuarioArea.getArea() != null && usuarioArea.getArea().getSoporteFijo() != null) {
            Usuario fijo = usuarioArea.getArea().getSoporteFijo();
            if (Boolean.TRUE.equals(fijo.getActivo()) && Boolean.TRUE.equals(fijo.getDisponibleSoporte())) {
                return fijo;
            }
        }

        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && "SOPORTE".equals(u.getRol().getNombre()) && Boolean.TRUE.equals(u.getDisponibleSoporte()))
                .min((u1, u2) -> {
                    long carga1 = ticketRepository.countByUsuarioSoporteAndEstatusNombre(u1, "ABIERTO");
                    long carga2 = ticketRepository.countByUsuarioSoporteAndEstatusNombre(u2, "ABIERTO");
                    return Long.compare(carga1, carga2);
                }).orElse(null);
    }

    public List<TicketResponse> obtenerTodosLosTickets() {
        return ticketRepository.findAll().stream()
                .map(this::mapearATicketResponse)
                .toList();
    }

    public List<TicketResponse> obtenerTicketsSegunRol(Long usuarioSolicitanteId) {
        Usuario usuario = usuarioRepository.findById(usuarioSolicitanteId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String nivel = usuario.getRol().getNivelVision(); 
        TicketFiltroStrategy estrategia = estrategiasFiltro.get(nivel);

        if (estrategia == null) {
            throw new RuntimeException("Error crítico: No existe una estrategia programada para el nivel de visión: " + nivel);
        }

        return estrategia.obtenerTickets(usuario).stream()
                .map(this::mapearATicketResponse)
                .toList();
    }

    private TicketResponse mapearATicketResponse(Ticket t) {
        String nombreSolicitante = "Desconocido";
        String nombreDepartamento = "Sin área";

        if (t.getUsuarioArea() != null) {
            nombreSolicitante = t.getUsuarioArea().getNombre();
            if (t.getUsuarioArea().getArea() != null) {
                nombreDepartamento = t.getUsuarioArea().getArea().getNombre();
            }
        }

        String justificacion = null;
        var historial = bitacoraRepository.findByTicketIdOrderByFechaCreacionDesc(t.getId());
        if (!historial.isEmpty()) {
            justificacion = historial.get(0).getJustificacion();
        }
        return new TicketResponse(
                t.getId(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getSede(),
                t.getFechaCreacion(),
                t.getFechaFin(),
                nombreSolicitante,
                nombreDepartamento,
                t.getEstatus().getNombre(),
                t.getUsuarioArea()!= null ? t.getUsuarioArea().getId() : null,
                t.getUsuarioSoporte() != null ? t.getUsuarioSoporte().getId() : null,
                justificacion
        );
    }

    @Transactional
    public Ticket finalizarTicketPorEmpleado(Long ticketId, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        if (!ticket.getUsuarioArea().getArea().getId().equals(usuario.getArea().getId())) {
            throw new SecurityException("No tienes permiso: El ticket pertenece a otra área.");
        }
    
        ticket.setEstatus(estatusRepository.findByNombre("CERRADO").orElseThrow());
        ticket.setFechaFin(LocalDateTime.now());
        Ticket ticketGuardado = ticketRepository.save(ticket);

        // NUEVO: Emitir actualización de estado al frontend
        try {
            messagingTemplate.convertAndSend("/topic/tickets-soporte", mapearATicketResponse(ticketGuardado));
        } catch (Exception e) {}

        return ticketGuardado;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> obtenerTicketsDeMiArea(String correoUsuario) {
        Usuario empleado = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (empleado.getArea() == null) throw new IllegalStateException("Usuario sin área.");

        return ticketRepository.findByUsuarioAreaAreaIdOrderByFechaCreacionDesc(empleado.getArea().getId())
                .stream()
                .map(this::mapearATicketResponse)
                .toList();
    }

    @Transactional
    public Ticket atenderTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Estatus estatusEnCamino = estatusRepository.findByNombre("EN PROCESO")
                .orElseThrow(() -> new IllegalStateException("El estatus EN PROCESO no existe en la BD."));

        ticket.setEstatus(estatusEnCamino);
        Ticket ticketGuardado = ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticketGuardado);
        bitacora.setEstatusRegistrado("EN PROCESO");
        bitacora.setJustificacion("El técnico va en camino para atender el reporte.");
        bitacoraRepository.save(bitacora);

        // NUEVO: Emitir actualización de estado al frontend
        try {
            messagingTemplate.convertAndSend("/topic/tickets-soporte", mapearATicketResponse(ticketGuardado));
        } catch (Exception e) {}

        return ticketGuardado;
    }   

    @Transactional
    public Ticket resolverTicket(Long ticketId, String justificacion) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Estatus estatusResuelto = estatusRepository.findByNombre("CERRADO")
                .orElseThrow(() -> new IllegalStateException("El estatus CERRADO no existe en la BD."));

        ticket.setEstatus(estatusResuelto);
        ticket.setFechaFin(LocalDateTime.now()); 
        Ticket ticketGuardado = ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticketGuardado);
        bitacora.setEstatusRegistrado("CERRADO");
        bitacora.setJustificacion(justificacion);
        bitacoraRepository.save(bitacora);

        // NUEVO: Emitir actualización de estado al frontend
        try {
            messagingTemplate.convertAndSend("/topic/tickets-soporte", mapearATicketResponse(ticketGuardado));
        } catch (Exception e) {}

        return ticketGuardado;
    }  

    @Transactional(readOnly = true)
    public List<TicketResponse> obtenerTicketsParaBandeja(String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.getRol() != null && "SOPORTE".equals(usuario.getRol().getNombre())) {
            return ticketRepository.findByUsuarioSoporteId(usuario.getId())
                    .stream()
                    .map(this::mapearATicketResponse)
                    .toList();
        }
        
        return obtenerTodosLosTickets();
    }
}