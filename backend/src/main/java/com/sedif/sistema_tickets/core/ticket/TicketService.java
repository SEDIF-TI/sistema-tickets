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
        // 1. Buscar al usuario solicitante
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Buscar Estatus (Estrategia de negocio)
        Estatus estatusAbierto = estatusRepository.findByNombre("ABIERTO")
                .orElseThrow(() -> new IllegalStateException("Estatus ABIERTO no configurado en la base de datos"));

        // 3. Construcción del Ticket
        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(request.titulo());
        nuevoTicket.setDescripcion(request.descripcion());
        nuevoTicket.setSede(request.sede());
        nuevoTicket.setUsuarioArea(usuario); 
        nuevoTicket.setEstatus(estatusAbierto); 
        nuevoTicket.setFechaCreacion(LocalDateTime.now());
        nuevoTicket.setCreadoPor(usuario.getCorreo());
        
        // 4. Asignación de Prioridad (Con validación robusta)
        String prioridad = (request.prioridad() != null && !request.prioridad().isBlank()) 
                           ? request.prioridad() 
                           : "NORMAL"; 
        nuevoTicket.setPrioridad(prioridad);

        // 5. LÓGICA UNIFICADA DE ASIGNACIÓN (Auto-asignación vs Balanceador)
        if (usuario.getRol() != null && "SOPORTE".equals(usuario.getRol().getNombre())) {
            // REGLA: Si quien levanta el ticket es de soporte, se lo auto-asigna
            nuevoTicket.setUsuarioSoporte(usuario);
            System.out.println("DEBUG: Ticket auto-asignado al técnico creador: " + usuario.getNombre());
        } else {
            // REGLA: Si es un empleado de otra área, usamos el balanceador de cargas
            Usuario soporteAsignado = resolverAsignacion(usuario);
            if (soporteAsignado != null) {
                nuevoTicket.setUsuarioSoporte(soporteAsignado);
                System.out.println("DEBUG: Ticket #" + nuevoTicket.getId() + " asignado por balanceador a: " + soporteAsignado.getNombre());
            } else {
                System.err.println("WARNING: Ticket creado sin técnico asignado.");
            }
        }

        // 6. Persistencia (¡Se guarda una sola vez!)
        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        // 7. Notificación automática por Telegram
        if (ticketGuardado.getUsuarioSoporte() != null && 
            ticketGuardado.getUsuarioSoporte().getTelegramChatId() != null) {
            
            String mensaje = "🚨 *NUEVO TICKET ASIGNADO* 🚨\n\n" +
                             "🆔 *ID:* #" + ticketGuardado.getId() + "\n" +
                             "📌 *Título:* " + ticketGuardado.getTitulo();
            
            try {
                telegramBot.enviarMensaje(ticketGuardado.getUsuarioSoporte().getTelegramChatId(), mensaje);
                System.out.println("✅ [SISTEMA] Notificación de Telegram enviada con éxito.");
            } catch (Exception e) {
                System.err.println("❌ [SISTEMA] Error al enviar notificación a Telegram: " + e.getMessage());
            }
        }

        // 8. Retorno final correcto
        return ticketGuardado;
    }

    private Usuario resolverAsignacion(Usuario usuarioArea) {
        if (usuarioArea.getArea() != null && usuarioArea.getArea().getSoporteFijo() != null) {
            Usuario fijo = usuarioArea.getArea().getSoporteFijo();
            // Validamos que el soporte fijo siga activo y disponible
            if (Boolean.TRUE.equals(fijo.getActivo()) && Boolean.TRUE.equals(fijo.getDisponibleSoporte())) {
                return fijo;
            }
        }

        return usuarioRepository.findAll().stream()
                // Comparamos el nombre del rol en la entidad, ya no usamos Enum
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

    // Método privado para centralizar el mapeo y evitar repetir código
    private TicketResponse mapearATicketResponse(Ticket t) {
        // 1. Manejo seguro de nulos para los nombres
        String nombreSolicitante = "Desconocido";
        String nombreDepartamento = "Sin área";

        if (t.getUsuarioArea() != null) {
            nombreSolicitante = t.getUsuarioArea().getNombre();
            if (t.getUsuarioArea().getArea() != null) {
                nombreDepartamento = t.getUsuarioArea().getArea().getNombre();
            }
        }

        // Buscamos si existe una justificación en la tabla Bitácora
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

        // Seguridad: Filtro invisible por Área
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

        // Aplicamos el filtro invisible de base de datos
        // En tu método obtenerTicketsDeMiArea:
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

        // Generamos el registro en la bitácora
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
        ticket.setFechaFin(LocalDateTime.now()); // <-- SE ASIGNA LA HORA EXACTA DE RESOLUCIÓN
        ticketRepository.save(ticket);

        // Guardamos la justificación técnica
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

        // Si es soporte, le damos SOLO los tickets que se le asignaron por el balanceador o él mismo
        if (usuario.getRol() != null && "SOPORTE".equals(usuario.getRol().getNombre())) {
            return ticketRepository.findByUsuarioSoporteId(usuario.getId())
                    .stream()
                    .map(this::mapearATicketResponse)
                    .toList();
        }
        
        // Si es Administrador, le damos todos
        return obtenerTodosLosTickets();
    }
}