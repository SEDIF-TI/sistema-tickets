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

/**
 * Emision de los documentos oficiales del area de soporte.
 *
 * <p>Cada endpoint compone un PDF (o una hoja de Excel) y lo devuelve como
 * flujo de bytes con la cabecera {@code Content-Disposition} adecuada: los
 * formatos se muestran en el navegador y el reporte de Excel se descarga.</p>
 *
 * <p>Todos llevan membrete institucional, de modo que su emision queda
 * restringida al personal del area de TI.</p>
 */
@RestController
@RequestMapping("/api/v1/documentos")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
@RequiredArgsConstructor
@Slf4j
public class DocumentoResource {

    private final DocumentoService documentoService;
    private final TicketRepository ticketRepository;
    private final ActividadExtraRepository actividadExtraRepository;
    private final DictamenService dictamenService;

    /**
     * Emite el dictamen tecnico: compone el PDF y lo registra en el historial.
     *
     * <p>Ambas cosas ocurren en la misma llamada, de modo que el historial
     * contiene exactamente los documentos entregados. El archivo se nombra con
     * el folio asignado en el registro.</p>
     *
     * <p>El fallo del registro no cancela la respuesta: se deja constancia en el
     * log y el PDF se entrega igualmente, nombrado con el folio del ticket.</p>
     */
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

    /** Vale de entrada del equipo que se recibe en el taller. */
    @PostMapping(value = "/entrada-equipo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarEntradaEquipo(@Valid @RequestBody EntradaEquipoRequest request) {
        byte[] pdfGenerado = documentoService.generarEntradaEquipoPdf(request);
        String folio = request.getFolioTicket() != null ? String.valueOf(request.getFolioTicket()) : "0000";
        return construirRespuestaPdf(pdfGenerado, "Entrada_Equipo_" + folio + ".pdf");
    }

    /**
     * Responsiva de resguardo lista para firma.
     *
     * <p>Solo compone el documento. El alta del prestamo la registra
     * {@code ResguardoResource}, que la pantalla invoca antes de pedir el PDF:
     * emitir una responsiva de un resguardo que no llego a guardarse dejaria un
     * papel firmado sin respaldo.</p>
     */
    @PostMapping(value = "/resguardos", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarResguardo(@Valid @RequestBody ResguardoRequest request) {
        byte[] pdfGenerado = documentoService.generarResguardo(request);
        return construirRespuestaPdf(pdfGenerado, "Resguardo_" + request.solicitanteNombre() + ".pdf");
    }

    /**
     * Reporte de actividades de un periodo, en PDF.
     *
     * <p>Reune los tickets cerrados dentro del rango y las actividades
     * registradas a mano. El alcance depende del rol que llega en el cuerpo: un
     * ADMINISTRADOR obtiene los de toda la institucion y cualquier otro los
     * suyos.</p>
     *
     * <p>Va en transaccion de solo lectura porque el generador atraviesa
     * relaciones perezosas —{@code ticket.usuarioArea.area} y
     * {@code actividad.usuario.area}— mientras compone el documento, y necesita
     * la sesion de Hibernate abierta hasta el final.</p>
     */
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

    /**
     * El mismo reporte de actividades en formato de hoja de calculo.
     *
     * <p>Comparte criterio de seleccion con la version en PDF, y tambien la
     * transaccion de solo lectura: el volcado a Excel lee el area del usuario
     * sobre relaciones perezosas.</p>
     */
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

    /**
     * Respuesta con el PDF en linea, para que el navegador lo muestre en su
     * visor en lugar de descargarlo directamente.
     */
    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] documento, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF); 
        headers.add("Content-Disposition", "inline; filename=\"" + nombreArchivo + "\"");
        return ResponseEntity.ok().headers(headers).body(documento);
    }

    /**
     * Respuesta con la hoja de calculo como adjunto: el navegador no dispone de
     * visor propio, de modo que se descarga.
     */
    private ResponseEntity<byte[]> construirRespuestaExcel(byte[] documento, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.add("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
        return ResponseEntity.ok().headers(headers).body(documento);
    }
    
}