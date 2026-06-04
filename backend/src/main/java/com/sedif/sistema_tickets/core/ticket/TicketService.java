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
        
        // 1. Buscamos al usuario que está logueado haciendo la petición
        Usuario usuario = usuarioRepository.findByCorreoOrUsername(identificadorUsuario, identificadorUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // CORRECCIÓN 1: Buscamos la entidad Estatus real en la base de datos
        Estatus estatusAbierto = estatusRepository.findByNombre("ABIERTO")
                .orElseThrow(() -> new IllegalStateException("Error del sistema: El estatus ABIERTO no está configurado en la BD."));

        // 2. Creamos la base del ticket
        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(request.titulo());
        nuevoTicket.setDescripcion(request.descripcion());
        
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
        return new TicketResponse(
                t.getId(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getEstatus().getId(),
                t.getUsuarioArea().getId(),
                t.getUsuarioSoporte() != null ? t.getUsuarioSoporte().getId() : null
        );
    }
}