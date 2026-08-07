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
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.actividad.ActividadExtra;
import com.sedif.sistema_tickets.core.actividad.ActividadExtraRepository;
import com.sedif.sistema_tickets.core.resguardo.ResguardoRequest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
    public ResponseEntity<byte[]> generarReporteActividades(
            @Valid @RequestBody ReporteActividadesRequest request) {

        DatosReporte datos = reunirDatosDelReporte(request);

        byte[] pdfBytes = documentoService.generarReporteActividadesPdf(
                request.fechaInicio(), request.fechaFin(), datos.tickets(), datos.actividades());

        return construirRespuestaPdf(pdfBytes, nombreDelReporte(request, ".pdf"));
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
    public ResponseEntity<byte[]> generarReporteExcel(
            @Valid @RequestBody ReporteActividadesRequest request) {

        DatosReporte datos = reunirDatosDelReporte(request);

        byte[] excelBytes = documentoService.generarReporteActividadesExcel(
                datos.tickets(), datos.actividades());

        return construirRespuestaExcel(excelBytes, nombreDelReporte(request, ".xlsx"));
    }

    /** Contenido del reporte, comun a los dos formatos. */
    private record DatosReporte(List<Ticket> tickets, List<ActividadExtra> actividades) {}

    /**
     * Reune los tickets cerrados y las actividades manuales del periodo.
     *
     * <p>El alcance depende del rol: ADMINISTRADOR abarca toda la institucion y
     * cualquier otro se limita al usuario indicado. Un rol que no figure en
     * {@link RolUsuario} recibe el trato restringido, que es el seguro.</p>
     *
     * <p>El recorte por estado y por fecha lo aplica la consulta, de modo que
     * solo viajan desde la base las filas que el documento imprime.</p>
     */
    private DatosReporte reunirDatosDelReporte(ReporteActividadesRequest request) {
        request.validarPeriodo();

        LocalDateTime inicioDia = request.fechaInicio().atStartOfDay();
        LocalDateTime finDia = request.fechaFin().atTime(LocalTime.MAX);

        if (RolUsuario.ADMINISTRADOR.es(request.rol())) {
            return new DatosReporte(
                    ticketRepository.buscarCerradosEnPeriodo(EstadoTicket.CERRADO, inicioDia, finDia),
                    actividadExtraRepository.findByFechaActividadBetween(inicioDia, finDia));
        }

        Long usuarioId = request.usuarioId() != null ? request.usuarioId() : 0L;

        return new DatosReporte(
                ticketRepository.buscarCerradosEnPeriodoPorTecnico(
                        EstadoTicket.CERRADO, usuarioId, inicioDia, finDia),
                actividadExtraRepository.findByUsuario_IdAndFechaActividadBetween(
                        usuarioId, inicioDia, finDia));
    }

    /** Nombre del archivo, con el periodo que abarca. */
    private String nombreDelReporte(ReporteActividadesRequest request, String extension) {
        return "Reporte_Actividades_" + request.fechaInicio()
                + "_al_" + request.fechaFin() + extension;
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