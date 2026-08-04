package com.sedif.sistema_tickets.core.dashboard;

import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.aviso.AvisoRepository;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
import com.sedif.sistema_tickets.util.enums.Prioridad;
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

    /**
     * Convierte los pares (etiqueta, conteo) que devuelven las consultas
     * agregadas.
     *
     * <p>Los valores que llegan como enum se muestran por su etiqueta legible:
     * la grafica de estados dibujaria si no "EN_PROCESO", con guion bajo, en
     * lugar de "En proceso".</p>
     */
    private List<DashboardResponse.MetricaGenerica> mapearGenerico(List<Object[]> datos) {
        return datos.stream()
            .map(obj -> DashboardResponse.MetricaGenerica.builder()
                .nombre(nombreLegible(obj[0]))
                .cantidad((Long) obj[1])
                .build())
            .collect(Collectors.toList());
    }

    private String nombreLegible(Object valor) {
        if (valor == null) {
            return "Sin asignar";
        }
        if (valor instanceof EstadoTicket estado) {
            return estado.getEtiqueta();
        }
        if (valor instanceof Prioridad prioridad) {
            return prioridad.getEtiqueta();
        }

        // La consulta de prioridades agrupa por la columna y devuelve texto,
        // no la constante del enum, asi que se traduce aqui: sin esto la
        // grafica mostraba "URGENTE" en mayusculas junto a estados ya
        // capitalizados como "Abierto".
        String texto = valor.toString();
        return Prioridad.desde(texto)
                .map(Prioridad::getEtiqueta)
                .orElse(texto);
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