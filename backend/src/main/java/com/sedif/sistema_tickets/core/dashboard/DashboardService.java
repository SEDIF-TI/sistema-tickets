package com.sedif.sistema_tickets.core.dashboard;

import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardResponse obtenerMetricas() {
        return DashboardResponse.builder()
            .totalTickets(ticketRepository.count())
            .totalUsuarios(usuarioRepository.count())
            .totalBitacora(150)
            .porEstatus(mapearGenerico(ticketRepository.contarPorEstatus()))
            .porArea(mapearGenerico(ticketRepository.contarTicketsPorArea()))
            .porIngeniero(mapearGenerico(ticketRepository.contarPorIngeniero()))
            .porPrioridad(mapearGenerico(ticketRepository.contarPorPrioridad()))
            .porFecha(mapearFecha(ticketRepository.contarPorFecha())) // <-- ¡AQUÍ ESTÁ LA PIEZA FALTANTE!
            .build();
    }

    // Mapeo para Estatus, Área, Ingeniero, etc.
    private List<DashboardResponse.MetricaGenerica> mapearGenerico(List<Object[]> datos) {
        return datos.stream()
            .map(obj -> DashboardResponse.MetricaGenerica.builder()
                .nombre(obj[0] != null ? obj[0].toString() : "Sin asignar")
                .cantidad((Long) obj[1])
                .build())
            .collect(Collectors.toList());
    }

    // Mapeo exclusivo para las fechas
    private List<DashboardResponse.MetricaFecha> mapearFecha(List<Object[]> datos) {
        return datos.stream()
            .map(obj -> DashboardResponse.MetricaFecha.builder()
                .fecha(obj[0] != null ? obj[0].toString() : "Sin fecha")
                .total((Long) obj[1])
                .resueltos(0L) // Puedes llenarlo después si quieres otra línea en la gráfica
                .build())
            .collect(Collectors.toList());
    }
}