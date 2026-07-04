package com.sedif.sistema_tickets.core.documento;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.actividad.ActividadExtra;
import com.sedif.sistema_tickets.core.actividad.ActividadExtraRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors; // <-- IMPORTANTE PARA EL NUEVO FILTRO

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoResource {

    private final DocumentoService documentoService;
    private final TicketRepository ticketRepository; 
    private final ActividadExtraRepository actividadExtraRepository;

    @PostMapping(value = "/memorandum", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarMemorandum(@RequestBody MemorandumRequest request) {
        byte[] pdfGenerado = documentoService.generarMemorandum(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Memorandum_Oficial.pdf");
        return ResponseEntity.ok().headers(headers).body(pdfGenerado);
    }

    @PostMapping(value = "/requisicion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarRequisicion(@RequestBody RequisicionRequest request) {
        byte[] pdfGenerado = documentoService.generarRequisicion(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Requisicion_Material.pdf");
        return ResponseEntity.ok().headers(headers).body(pdfGenerado);
    }

    @PostMapping(value = "/dictamen", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarDictamen(@RequestBody DictamenRequest request) {
        byte[] pdfGenerado = documentoService.generarDictamenTecnicoPdf(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Dictamen_Tecnico_" + request.folioTicket() + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfGenerado);
    }

    // ==========================================================
    // MÉTODO PARA GENERAR EL REPORTE EN PDF
    // ==========================================================
    @PostMapping("/reporte-actividades")
    public ResponseEntity<byte[]> generarReporteActividades(@RequestBody Map<String, Object> payload) {
        LocalDate fechaInicio = LocalDate.parse(payload.get("fechaInicio").toString());
        LocalDate fechaFin = LocalDate.parse(payload.get("fechaFin").toString());
        LocalDateTime inicioDia = fechaInicio.atStartOfDay();
        LocalDateTime finDia = fechaFin.atTime(LocalTime.MAX);

        String rol = payload.containsKey("rol") && payload.get("rol") != null ? payload.get("rol").toString() : "";
        Long usuarioId = payload.containsKey("usuarioId") && payload.get("usuarioId") != null ? Long.parseLong(payload.get("usuarioId").toString()) : 0L;

        List<Ticket> ticketsCerrados;
        List<ActividadExtra> actividadesExtra;

        // --- FILTRO INFALIBLE EN MEMORIA CON JAVA STREAMS ---
        if ("ADMINISTRADOR".equalsIgnoreCase(rol)) {
            // Admin: Traemos todos y filtramos en Java
            ticketsCerrados = ticketRepository.findAll().stream()
                .filter(t -> t.getEstatus() != null && t.getEstatus().getId() == 5L)
                .filter(t -> t.getFechaFin() != null)
                .filter(t -> !t.getFechaFin().isBefore(inicioDia) && !t.getFechaFin().isAfter(finDia))
                .collect(Collectors.toList());

            actividadesExtra = actividadExtraRepository.findByFechaActividadBetween(inicioDia, finDia);
        } else {
            // Soporte: Traemos los de este técnico y filtramos en Java
            ticketsCerrados = ticketRepository.findByUsuarioSoporteId(usuarioId).stream()
                .filter(t -> t.getEstatus() != null && t.getEstatus().getId() == 5L)
                .filter(t -> t.getFechaFin() != null)
                .filter(t -> !t.getFechaFin().isBefore(inicioDia) && !t.getFechaFin().isAfter(finDia))
                .collect(Collectors.toList());

            actividadesExtra = actividadExtraRepository.findByUsuario_IdAndFechaActividadBetween(usuarioId, inicioDia, finDia);
        }

        byte[] pdfBytes = documentoService.generarReporteActividadesPdf(fechaInicio, fechaFin, ticketsCerrados, actividadesExtra);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // ==========================================================
    // MÉTODO PARA GENERAR EL REPORTE EN EXCEL
    // ==========================================================
    @PostMapping("/reporte-actividades/excel")
    public ResponseEntity<byte[]> generarReporteExcel(@RequestBody Map<String, Object> payload) {
        LocalDate fechaInicio = LocalDate.parse(payload.get("fechaInicio").toString());
        LocalDate fechaFin = LocalDate.parse(payload.get("fechaFin").toString());
        LocalDateTime inicioDia = fechaInicio.atStartOfDay();
        LocalDateTime finDia = fechaFin.atTime(LocalTime.MAX);

        String rol = payload.containsKey("rol") && payload.get("rol") != null ? payload.get("rol").toString() : "";
        Long usuarioId = payload.containsKey("usuarioId") && payload.get("usuarioId") != null ? Long.parseLong(payload.get("usuarioId").toString()) : 0L;

        List<Ticket> ticketsCerrados;
        List<ActividadExtra> actividadesExtra;

        // --- FILTRO INFALIBLE EN MEMORIA CON JAVA STREAMS ---
        if ("ADMINISTRADOR".equalsIgnoreCase(rol)) {
            ticketsCerrados = ticketRepository.findAll().stream()
                .filter(t -> t.getEstatus() != null && t.getEstatus().getId() == 5L)
                .filter(t -> t.getFechaFin() != null)
                .filter(t -> !t.getFechaFin().isBefore(inicioDia) && !t.getFechaFin().isAfter(finDia))
                .collect(Collectors.toList());

            actividadesExtra = actividadExtraRepository.findByFechaActividadBetween(inicioDia, finDia);
        } else {
            ticketsCerrados = ticketRepository.findByUsuarioSoporteId(usuarioId).stream()
                .filter(t -> t.getEstatus() != null && t.getEstatus().getId() == 5L)
                .filter(t -> t.getFechaFin() != null)
                .filter(t -> !t.getFechaFin().isBefore(inicioDia) && !t.getFechaFin().isAfter(finDia))
                .collect(Collectors.toList());

            actividadesExtra = actividadExtraRepository.findByUsuario_IdAndFechaActividadBetween(usuarioId, inicioDia, finDia);
        }

        byte[] excelBytes = documentoService.generarReporteActividadesExcel(ticketsCerrados, actividadesExtra);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.add("Content-Disposition", "attachment; filename=Reporte_Actividades.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}