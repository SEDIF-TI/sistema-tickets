package com.sedif.sistema_tickets.core.dashboard;

import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TicketRepository ticketRepository;

    public DashboardResponse obtenerMetricas() {
        // 1. Cálculos generales basados en el nombre del estatus relacional
        long total = ticketRepository.count();
        long resueltos = ticketRepository.countByEstatusNombreIgnoreCase("CERRADO");
        long pendientes = ticketRepository.countByEstatusNombreNotIgnoreCase("CERRADO");

        // 2. Mapeo de la consulta grupal por áreas (Object[]) hacia el DTO interno
        List<Object[]> resultadosArea = ticketRepository.contarTicketsPorArea();
        
        List<DashboardResponse.AreaMetrica> metricaAreas = resultadosArea.stream()
                .map(obj -> DashboardResponse.AreaMetrica.builder()
                        .nombre((String) obj[0])
                        .cantidad((Long) obj[1])
                        .build())
                .collect(Collectors.toList());

        // 3. Retorno del empaquetado final
        return DashboardResponse.builder()
                .totalTickets(total)
                .resueltos(resueltos)
                .pendientes(pendientes)
                .ticketsPorArea(metricaAreas)
                .build();
    }
}