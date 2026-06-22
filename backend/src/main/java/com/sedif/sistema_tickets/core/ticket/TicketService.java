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
        
        // ---> GUARDAMOS EL NOMBRE DEL SOLICITANTE FÍSICO
        nuevoTicket.setSolicitanteNombre(request.solicitante()); 
        
        nuevoTicket.setUsuarioArea(usuario); 
        nuevoTicket.setEstatus(estatusAbierto); 
        nuevoTicket.setFechaCreacion(LocalDateTime.now());
        nuevoTicket.setCreadoPor(usuario.getCorreo());
        
        String prioridad = (request.prioridad() != null && !request.prioridad().isBlank()) 
                           ? request.prioridad() 
                           : "NORMAL"; 
        nuevoTicket.setPrioridad(prioridad);

        if (usuario.getRol() != null && "SOPORTE".equals(usuario.getRol().getNombre())) {
            nuevoTicket.setUsuarioSoporte(usuario);
            System.out.println("DEBUG: Ticket auto-asignado al técnico creador: " + usuario.getNombre());
        } else {
            Usuario soporteAsignado = resolverAsignacion(usuario);
            if (soporteAsignado != null) {
                nuevoTicket.setUsuarioSoporte(soporteAsignado);
                System.out.println("DEBUG: Ticket #" + nuevoTicket.getId() + " asignado por balanceador a: " + soporteAsignado.getNombre());
            } else {
                System.err.println("WARNING: Ticket creado sin técnico asignado.");
            }
        }

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        if (ticketGuardado.getUsuarioSoporte() != null && 
            ticketGuardado.getUsuarioSoporte().getTelegramChatId() != null) {
            
            String mensaje = "🚨 *NUEVO TICKET ASIGNADO* 🚨\n\n" +
                             "🆔 *ID:* #" + ticketGuardado.getId() + "\n" +
                             "👤 *Solicitante:* " + (ticketGuardado.getSolicitanteNombre() != null ? ticketGuardado.getSolicitanteNombre() : "N/A") + "\n" +
                             "📌 *Título:* " + ticketGuardado.getTitulo();
            
            try {
                telegramBot.enviarMensaje(ticketGuardado.getUsuarioSoporte().getTelegramChatId(), mensaje);
                System.out.println("✅ [SISTEMA] Notificación de Telegram enviada con éxito.");
            } catch (Exception e) {
                System.err.println("❌ [SISTEMA] Error al enviar notificación a Telegram: " + e.getMessage());
            }
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

        // ---> LOGICA DE PRIORIDAD PARA EL NOMBRE DEL SOLICITANTE
        if (t.getSolicitanteNombre() != null && !t.getSolicitanteNombre().isBlank()) {
            nombreSolicitante = t.getSolicitanteNombre(); // Prioridad 1: Nombre real de quien tiene la falla
        } else if (t.getUsuarioArea() != null) {
            nombreSolicitante = t.getUsuarioArea().getNombre(); // Prioridad 2: Fallback al dueño de la cuenta (Tickets viejos)
        }

        if (t.getUsuarioArea() != null && t.getUsuarioArea().getArea() != null) {
            nombreDepartamento = t.getUsuarioArea().getArea().getNombre();
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
        return ticketRepository.save(ticket);
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
        ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticket);
        bitacora.setEstatusRegistrado("EN PROCESO");
        bitacora.setJustificacion("El técnico va en camino para atender el reporte.");
        bitacoraRepository.save(bitacora);

        return ticket;
    }

    @Transactional
    public Ticket resolverTicket(Long ticketId, String justificacion) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Estatus estatusResuelto = estatusRepository.findByNombre("CERRADO")
                .orElseThrow(() -> new IllegalStateException("El estatus CERRADO no existe en la BD."));

        ticket.setEstatus(estatusResuelto);
        ticket.setFechaFin(LocalDateTime.now());
        ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticket);
        bitacora.setEstatusRegistrado("CERRADO");
        bitacora.setJustificacion(justificacion);
        bitacoraRepository.save(bitacora);

        return ticket;
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