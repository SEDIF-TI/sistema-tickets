package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.estatusticket.Estatus;
import com.sedif.sistema_tickets.core.estatusticket.EstatusRepository;
import com.sedif.sistema_tickets.core.ticket.filtros.TicketFiltroStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public TicketResponse crearTicket(TicketRecord record) {
        Usuario usuarioArea = usuarioRepository.findById(record.usuarioAreaId())
                .orElseThrow(() -> new RuntimeException("Usuario de área no encontrado"));

        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(record.titulo());
        nuevoTicket.setDescripcion(record.descripcion());
        nuevoTicket.setUsuarioArea(usuarioArea);

        Estatus estatusInicial = estatusRepository.findByNombre("ABIERTO")
                .orElseThrow(() -> new RuntimeException("Estatus 'ABIERTO' no configurado en el sistema"));
        nuevoTicket.setEstatus(estatusInicial);

        Usuario soporteAsignado = resolverAsignacion(usuarioArea);
        nuevoTicket.setUsuarioSoporte(soporteAsignado);

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        return mapearATicketResponse(ticketGuardado);
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
            // CORREGIDO: Usamos la variable 'nivel' que sí existe
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