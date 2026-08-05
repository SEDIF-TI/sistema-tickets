package com.sedif.sistema_tickets.core.documento;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.sedif.sistema_tickets.core.dictamen.DictamenService;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
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
// Documentos con membrete institucional: solo el area de TI los emite.
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
@RequiredArgsConstructor
@Slf4j
public class DocumentoResource {

    private final DocumentoService documentoService;
    private final TicketRepository ticketRepository;
    private final ActividadExtraRepository actividadExtraRepository;
    private final DictamenService dictamenService;

    // ==========================================================
    // 1. GENERACIÓN DE DICTAMEN
    // ==========================================================
    // Desde la V10 el dictamen queda registrado al emitirse: antes se componia
    // el PDF y se devolvia sin dejar rastro, de modo que no habia historial que
    // consultar.
    @PostMapping(value = "/dictamen", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarDictamen(@Valid @RequestBody DictamenRequest request,
                                                  Authentication authentication) {
        byte[] pdfGenerado = documentoService.generarDictamenTecnicoPdf(request);

        // El registro no debe impedir la entrega del documento: si falla el
        // guardado, el tecnico se queda igualmente con su PDF y el fallo consta
        // en el log.
        String folio = null;
        try {
            folio = dictamenService.registrar(
                    request, authentication != null ? authentication.getName() : null).getFolio();
        } catch (Exception e) {
            log.error("El dictamen se genero pero no pudo registrarse en el historial.", e);
        }

        String nombre = folio != null
                ? "Dictamen_Tecnico_" + folio + ".pdf"
                : "Dictamen_Tecnico_" + request.folioTicket() + ".pdf";
        return construirRespuestaPdf(pdfGenerado, nombre);
    }

    // ==========================================================
    // 3. GENERACIÓN DE ENTRADA DE EQUIPO
    // ==========================================================
    @PostMapping(value = "/entrada-equipo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarEntradaEquipo(@Valid @RequestBody EntradaEquipoRequest request) {
        byte[] pdfGenerado = documentoService.generarEntradaEquipoPdf(request);
        String folio = request.getFolioTicket() != null ? String.valueOf(request.getFolioTicket()) : "0000";
        return construirRespuestaPdf(pdfGenerado, "Entrada_Equipo_" + folio + ".pdf");
    }

    // ==========================================================
    // 4. GENERACIÓN DE RESGUARDO (SOLO PDF)
    // ==========================================================
    @PostMapping(value = "/resguardos", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarResguardo(@Valid @RequestBody ResguardoRequest request) {
        // Este método usa tu lógica existente en DocumentoService para crear el PDF
        byte[] pdfGenerado = documentoService.generarResguardo(request);
        return construirRespuestaPdf(pdfGenerado, "Resguardo_" + request.solicitanteNombre() + ".pdf");
    }

    // ==========================================================
    // 5. GENERACIÓN DEL REPORTE DE ACTIVIDADES (PDF)
    // ==========================================================
    // La sesion de Hibernate debe seguir abierta mientras se compone el
    // documento: el generador recorre `ticket.usuarioArea.area` y
    // `actividad.usuario.area`, que son proxies perezosos. Sin transaccion, la
    // sesion se cerraba al volver del repositorio y el primer acceso a esas
    // relaciones lanzaba LazyInitializationException, de modo que el reporte
    // fallaba siempre.
    @Transactional(readOnly = true)
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
                .filter(t -> t.getEstado() == EstadoTicket.CERRADO)
                .filter(t -> t.getFechaFin() != null)
                .filter(t -> !t.getFechaFin().isBefore(inicioDia) && !t.getFechaFin().isAfter(finDia))
                .collect(Collectors.toList());
            actividadesExtra = actividadExtraRepository.findByFechaActividadBetween(inicioDia, finDia);
        } else {
            ticketsCerrados = ticketRepository.findByUsuarioSoporteId(usuarioId).stream()
                .filter(t -> t.getEstado() == EstadoTicket.CERRADO)
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
    // Mismo motivo que en el reporte en PDF: sin la transaccion abierta, leer
    // el area del usuario sobre un proxy ya desconectado rompia la generacion.
    @Transactional(readOnly = true)
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
                .filter(t -> t.getEstado() == EstadoTicket.CERRADO)
                .filter(t -> t.getFechaFin() != null)
                .filter(t -> !t.getFechaFin().isBefore(inicioDia) && !t.getFechaFin().isAfter(finDia))
                .collect(Collectors.toList());
            actividadesExtra = actividadExtraRepository.findByFechaActividadBetween(inicioDia, finDia);
        } else {
            ticketsCerrados = ticketRepository.findByUsuarioSoporteId(usuarioId).stream()
                .filter(t -> t.getEstado() == EstadoTicket.CERRADO)
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