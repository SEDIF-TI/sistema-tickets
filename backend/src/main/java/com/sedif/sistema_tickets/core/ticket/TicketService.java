package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.estatusticket.Estatus;           // Importar Estatus
import com.sedif.sistema_tickets.core.estatusticket.EstatusRepository; // Importar Repositorio
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstatusRepository estatusRepository;

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

    return new TicketResponse(
                    ticketGuardado.getId(),
                    ticketGuardado.getTitulo(),
                    ticketGuardado.getDescripcion(),
                    ticketGuardado.getEstatus().getId(), // <-- Ahora obtenemos el ID
                    ticketGuardado.getUsuarioArea().getId(),
                    ticketGuardado.getUsuarioSoporte() != null ? ticketGuardado.getUsuarioSoporte().getId() : null
            );
    }

    private Usuario resolverAsignacion(Usuario usuarioArea) {
        if (usuarioArea.getArea() != null && usuarioArea.getArea().getSoporteFijo() != null) {
            Usuario fijo = usuarioArea.getArea().getSoporteFijo();
            if (Boolean.TRUE.equals(fijo.getDisponibleSoporte())) {
                return fijo;
            }
        }

        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == RolUsuario.SOPORTE && Boolean.TRUE.equals(u.getDisponibleSoporte()))
                .min((u1, u2) -> {
                    long carga1 = ticketRepository.countByUsuarioSoporteAndEstatusNombre(u1, "ABIERTO");
                    long carga2 = ticketRepository.countByUsuarioSoporteAndEstatusNombre(u2, "ABIERTO");
                    return Long.compare(carga1, carga2);
                }).orElse(null);
    }


    public List<TicketResponse> obtenerTodosLosTickets() {
    return ticketRepository.findAll().stream()
            .map(ticket -> new TicketResponse(
                    ticket.getId(),
                    ticket.getTitulo(),
                    ticket.getDescripcion(),
                    ticket.getEstatus().getId(), // Obtenemos el ID del estado
                    ticket.getUsuarioArea().getId(),
                    ticket.getUsuarioSoporte() != null ? ticket.getUsuarioSoporte().getId() : null
            ))
            .toList();
    }
}