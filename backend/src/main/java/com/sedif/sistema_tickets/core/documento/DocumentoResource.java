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
import com.sedif.sistema_tickets.core.resguardo.ResguardoRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoResource {

    private final DocumentoService documentoService;
    private final TicketRepository ticketRepository; 
    private final ActividadExtraRepository actividadExtraRepository;

    // ==========================================================
    // 1. GENERACIÓN DE DICTAMEN
    // ==========================================================
    @PostMapping(value = "/dictamen", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarDictamen(@RequestBody DictamenRequest request) {
        byte[] pdfGenerado = documentoService.generarDictamenTecnicoPdf(request);
        return construirRespuestaPdf(pdfGenerado, "Dictamen_Tecnico_" + request.folioTicket() + ".pdf");
    }

    // ==========================================================
    // 2. GENERACIÓN DE MANTENIMIENTO PREVENTIVO
    // ==========================================================
    @PostMapping(value = "/mantenimiento", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarMantenimientoPreventivoPdf(@RequestBody MantenimientoPreventivoRequest request) {
        byte[] pdfGenerado = documentoService.generarMantenimientoPreventivoPdf(request);
        return construirRespuestaPdf(pdfGenerado, "Mantenimiento_Preventivo_" + request.departamento() + ".pdf");
    }

    // ==========================================================
    // 3. GENERACIÓN DE ENTRADA DE EQUIPO
    // ==========================================================
    @PostMapping(value = "/entrada-equipo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarEntradaEquipo(@RequestBody EntradaEquipoRequest request) {
        byte[] pdfGenerado = documentoService.generarEntradaEquipoPdf(request);
        String folio = request.getFolioTicket() != null ? String.valueOf(request.getFolioTicket()) : "0000";
        return construirRespuestaPdf(pdfGenerado, "Entrada_Equipo_" + folio + ".pdf");
    }

    // ==========================================================
    // 4. GENERACIÓN DE RESGUARDO (SOLO PDF)
    // ==========================================================
    @PostMapping(value = "/resguardos", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarResguardo(@RequestBody ResguardoRequest request) {
        // Este método usa tu lógica existente en DocumentoService para crear el PDF
        byte[] pdfGenerado = documentoService.generarResguardo(request);
        return construirRespuestaPdf(pdfGenerado, "Resguardo_" + request.solicitanteNombre() + ".pdf");
    }

    // ==========================================================
    // 5. GENERACIÓN DEL REPORTE DE ACTIVIDADES (PDF)
    // ==========================================================
    @PostMapping(value = "/reporte-actividades", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarReporteActividades(@RequestBody Map<String, Object> payload) {
        LocalDate fechaInicio = LocalDate.parse(payload.get("fechaInicio").toString());
        LocalDate fechaFin = LocalDate.parse(payload.get("fechaFin").toString());
        LocalDateTime inicioDia = fechaInicio.atStartOfDay();
        LocalDateTime finDia = fechaFin.atTime(LocalTime.MAX);

        String rol = payload.containsKey("rol") && payload.get("rol") != null ? payload.get("rol").toString() : "";
        Long usuarioId = payload.containsKey("usuarioId") && payload.get("usuarioId") != null ? Long.parseLong(payload.get("usuarioId").toString()) : 0L;

        List<Ticket> ticketsCerrados;
        List<ActividadExtra> actividadesExtra;

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

        byte[] pdfBytes = documentoService.generarReporteActividadesPdf(fechaInicio, fechaFin, ticketsCerrados, actividadesExtra);
        return construirRespuestaPdf(pdfBytes, "Reporte_Actividades_" + fechaInicio + "_al_" + fechaFin + ".pdf");
    }

    // ==========================================================
    // 6. GENERACIÓN DEL REPORTE DE ACTIVIDADES (EXCEL)
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
        return construirRespuestaExcel(excelBytes, "Reporte_Actividades_" + fechaInicio + "_al_" + fechaFin + ".xlsx");
    }

    // ==========================================================
    // MÉTODOS AUXILIARES PARA RESPUESTAS HTTP
    // ==========================================================
    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] documento, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF); 
        headers.add("Content-Disposition", "inline; filename=\"" + nombreArchivo + "\"");
        return ResponseEntity.ok().headers(headers).body(documento);
    }

    private ResponseEntity<byte[]> construirRespuestaExcel(byte[] documento, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.add("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
        return ResponseEntity.ok().headers(headers).body(documento);
    }
    
}