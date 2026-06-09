package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.estatusticket.Estatus;
import com.sedif.sistema_tickets.core.estatusticket.EstatusRepository;
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

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstatusRepository estatusRepository;
    private final Map<String, TicketFiltroStrategy> estrategiasFiltro;

    @Transactional
    public Ticket crearTicket(TicketRequestRecord request, String identificadorUsuario) {
        
    System.out.println("DEBUG: Intentando crear ticket para usuario con identificador: " + identificadorUsuario);
        
        // 1. Buscamos al usuario
        Usuario usuario = usuarioRepository.findByCorreoOrUsername(identificadorUsuario.trim(), identificadorUsuario.trim())
                .orElseGet(() -> {
                    System.err.println("DEBUG: Falló búsqueda exacta. Intentando buscar por correo en minúsculas...");
                    return usuarioRepository.findByCorreoOrUsername(identificadorUsuario.toLowerCase(), identificadorUsuario.toLowerCase())
                            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en BD: " + identificadorUsuario));
                });

        // CORRECCIÓN 1: Buscamos la entidad Estatus real en la base de datos
        Estatus estatusAbierto = estatusRepository.findByNombre("ABIERTO")
                .orElseThrow(() -> new IllegalStateException("Error del sistema: El estatus ABIERTO no está configurado en la BD."));

        // 2. Creamos la base del ticket
        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(request.titulo());
        nuevoTicket.setDescripcion(request.descripcion());

        nuevoTicket.setSede(request.sede());
        
        // CORRECCIÓN 2: Usamos los nombres correctos de tu entidad
        nuevoTicket.setUsuarioArea(usuario); 
        nuevoTicket.setEstatus(estatusAbierto); 
        
        nuevoTicket.setFechaCreacion(LocalDateTime.now());

        // 3. APLICAMOS LA REGLA DE NEGOCIO DE PRIORIDAD
        if (usuario.getArea() != null && Boolean.TRUE.equals(usuario.getArea().getPrioritaria())) {
            nuevoTicket.setPrioridad("ALTA");
        } else {
            nuevoTicket.setPrioridad("NORMAL");
        }

        // CORRECCIÓN 3: Ejecutamos tu balanceador para asignar al técnico adecuado
        Usuario soporteAsignado = resolverAsignacion(usuario);
        if (soporteAsignado != null) {
            nuevoTicket.setUsuarioSoporte(soporteAsignado);
        }

        // 4. Guardamos en la base de datos
        return ticketRepository.save(nuevoTicket);
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
        return new TicketResponse(
                t.getId(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getSede(),
                t.getFechaCreacion(),
                nombreSolicitante,
                nombreDepartamento,
                t.getEstatus().getNombre(),
                t.getUsuarioArea()!= null ? t.getUsuarioArea().getId() : null,
                t.getUsuarioSoporte() != null ? t.getUsuarioSoporte().getId() : null
        );
    }

    @Transactional
    public Ticket finalizarTicketPorEmpleado(Long ticketId, String correoUsuario) {
        // 1. Buscamos el ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        // 2. Seguridad: Validamos que quien intenta cerrar el ticket es quien lo creó
        if (!ticket.getCreadoPor().equals(correoUsuario)) {
            throw new SecurityException("No tienes permiso para finalizar este ticket. Solo el creador puede hacerlo.");
        }
    
        // 3. Buscamos el estatus final usando las clases correctas de tu proyecto
        Estatus estatusCerrado = estatusRepository.findByNombre("CERRADO") 
                .orElseThrow(() -> new IllegalArgumentException("El estatus CERRADO no existe en la base de datos"));

        // 4. Actualizamos y guardamos (usando el setter correcto)
        ticket.setEstatus(estatusCerrado);
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> obtenerTicketsDeMiArea(String correoUsuario) {
        // 1. Buscamos al usuario logueado
        Usuario empleado = usuarioRepository.findByCorreoOrUsername(correoUsuario, correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Verificamos que tenga un área asignada
        if (empleado.getArea() == null) {
            throw new IllegalStateException("El usuario no tiene un área asignada para ver el historial.");
        }

        // 3. Buscamos los tickets de su área y los mapeamos a la respuesta
        Long miAreaId = empleado.getArea().getId();
        return ticketRepository.findByUsuarioArea_Area_IdOrderByFechaCreacionDesc(miAreaId)
                .stream()
                .map(this::mapearATicketResponse)
                .toList();
    }
}