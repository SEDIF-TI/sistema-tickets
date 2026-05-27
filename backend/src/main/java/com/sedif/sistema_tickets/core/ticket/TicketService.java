package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Ticket crearTicket(TicketRecord record) {
        // 1. Buscamos al usuario que crea el ticket
        Usuario usuarioArea = usuarioRepository.findById(record.usuarioAreaId())
                .orElseThrow(() -> new RuntimeException("Usuario de área no encontrado"));

        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(record.titulo());
        nuevoTicket.setDescripcion(record.descripcion());
        nuevoTicket.setUsuarioArea(usuarioArea);

        // 2. Lógica de asignación automática
        Usuario soporteAsignado = resolverAsignacion(usuarioArea);
        nuevoTicket.setUsuarioSoporte(soporteAsignado);

        return ticketRepository.save(nuevoTicket);
    }

    private Usuario resolverAsignacion(Usuario usuarioArea) {
        // 1. Regla de Oro: Soporte Fijo
        if (usuarioArea.getArea() != null && usuarioArea.getArea().getSoporteFijo() != null) {
            Usuario fijo = usuarioArea.getArea().getSoporteFijo();
            if (Boolean.TRUE.equals(fijo.getDisponibleSoporte())) {
                return fijo;
            }
        }

        // 2. Plan B: Asignación equitativa
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == RolUsuario.SOPORTE && Boolean.TRUE.equals(u.getDisponibleSoporte()))
                .min((u1, u2) -> {
                    // Usamos countByUsuarioSoporteAndEstatus para contar los tickets abiertos
                    long carga1 = ticketRepository.countByUsuarioSoporteAndEstatus(u1, "ABIERTO");
                    long carga2 = ticketRepository.countByUsuarioSoporteAndEstatus(u2, "ABIERTO");
                    return Long.compare(carga1, carga2);
                })
                .orElseThrow(() -> new RuntimeException("No hay técnicos de soporte disponibles en este momento."));
    }
}