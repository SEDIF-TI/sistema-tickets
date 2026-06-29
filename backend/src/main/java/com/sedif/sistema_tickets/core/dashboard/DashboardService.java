package com.sedif.sistema_tickets.core.dashboard;

import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.aviso.AvisoRepository; // <-- Importante
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvisoRepository avisoRepository; // ---> NUEVO: Inyectamos el repositorio de Avisos

    public DashboardResponse obtenerMetricas() {
        return DashboardResponse.builder()
            .totalTickets(ticketRepository.count())
            .totalUsuarios(usuarioRepository.count())
            .totalBitacora(150)
            .porEstatus(mapearGenerico(ticketRepository.contarPorEstatus()))
            .porArea(mapearGenerico(ticketRepository.contarTicketsPorArea()))
            .porIngeniero(mapearGenerico(ticketRepository.contarPorIngeniero()))
            .porPrioridad(mapearGenerico(ticketRepository.contarPorPrioridad()))
            .porFecha(mapearFecha(ticketRepository.contarPorFecha())) 
            // ---> NUEVO: Mapeamos los avisos activos agrupados por área
            .avisosPorArea(mapearGenerico(avisoRepository.contarAvisosActivosPorArea())) 
            .build();
    }

    private List<DashboardResponse.MetricaGenerica> mapearGenerico(List<Object[]> datos) {
        return datos.stream()
            .map(obj -> DashboardResponse.MetricaGenerica.builder()
                .nombre(obj[0] != null ? obj[0].toString() : "Sin asignar")
                .cantidad((Long) obj[1])
                .build())
            .collect(Collectors.toList());
    }

    private List<DashboardResponse.MetricaFecha> mapearFecha(List<Object[]> datos) {
        return datos.stream()
            .map(obj -> DashboardResponse.MetricaFecha.builder()
                .fecha(obj[0] != null ? obj[0].toString() : "Sin fecha")
                .total((Long) obj[1])
                .resueltos(0L) 
                .build())
            .collect(Collectors.toList());
    }
}