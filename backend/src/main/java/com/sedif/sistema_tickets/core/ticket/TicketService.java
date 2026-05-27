package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // 1. Fase de Enrutamiento Dedicado (Regla de Oro: Soporte Fijo de esa área)
        if (usuarioArea.getArea() != null && usuarioArea.getArea().getSoporteFijo() != null) {
            Usuario fijo = usuarioArea.getArea().getSoporteFijo();
            // Validamos que el soporte fijo siga activo y disponible
            if (Boolean.TRUE.equals(fijo.getActivo()) && Boolean.TRUE.equals(fijo.getDisponibleSoporte())) {
                return fijo;
            }
        }

        // 2. Fase de Desbordamiento y Asignación Equitativa (Round-Robin Global)
        // Pedimos estrictamente solo 1 registro (el de menor carga)
        List<Usuario> tecnicosDisponibles = usuarioRepository.buscarTecnicosGlobalesOrdenadosPorCarga(
                RolUsuario.SOPORTE, 
                "ABIERTO", 
                PageRequest.of(0, 1)
        );

        if (tecnicosDisponibles.isEmpty()) {
            throw new RuntimeException("No hay técnicos de soporte global disponibles en este momento para recibir el ticket.");
        }

        return tecnicosDisponibles.get(0);
    }
}