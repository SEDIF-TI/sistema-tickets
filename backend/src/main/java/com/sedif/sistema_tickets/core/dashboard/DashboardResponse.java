package com.sedif.sistema_tickets.core.dashboard;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalTickets;
    private long resueltos;
    private long pendientes;
    private List<AreaMetrica> ticketsPorArea;

    @Data
    @Builder
    public static class AreaMetrica {
        private String nombre;
        private long cantidad;
    }
}