package com.sedif.sistema_tickets.core.documento;

import com.sedif.sistema_tickets.core.actividad.ActividadExtra;
import com.sedif.sistema_tickets.core.documento.pdf.DictamenPdf;
import com.sedif.sistema_tickets.core.documento.pdf.EntradaEquipoPdf;
import com.sedif.sistema_tickets.core.documento.pdf.ReporteActividadesDocumento;
import com.sedif.sistema_tickets.core.documento.pdf.ResguardoPdf;
import com.sedif.sistema_tickets.core.resguardo.ResguardoRequest;
import com.sedif.sistema_tickets.core.ticket.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Punto de entrada a los documentos oficiales del area de soporte.
 *
 * <p>Cada formato se compone en su propia clase dentro del paquete
 * {@code documento.pdf}, y este servicio solo encamina la peticion hacia la que
 * corresponde. Asi, tocar la maquetacion de un documento no obliga a abrir el
 * codigo de los demas.</p>
 *
 * <ul>
 *   <li>{@link DictamenPdf} — dictamen tecnico.</li>
 *   <li>{@link ResguardoPdf} — responsiva de resguardo.</li>
 *   <li>{@link EntradaEquipoPdf} — vale de entrada al taller.</li>
 *   <li>{@link ReporteActividadesDocumento} — reporte del periodo, en PDF y en
 *       hoja de calculo.</li>
 * </ul>
 *
 * <p>Los formatos en PDF se arman con OpenPDF y el reporte en hoja de calculo
 * con Apache POI. Todos devuelven el documento como arreglo de bytes: nada se
 * escribe en disco.</p>
 */
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DictamenPdf dictamenPdf;
    private final ResguardoPdf resguardoPdf;
    private final EntradaEquipoPdf entradaEquipoPdf;
    private final ReporteActividadesDocumento reporteActividades;

    /** Dictamen tecnico del equipo revisado. */
    public byte[] generarDictamenTecnicoPdf(DictamenRequest request) {
        return dictamenPdf.generar(request);
    }

    /** Responsiva con la que un equipo queda bajo custodia de una persona. */
    public byte[] generarResguardo(ResguardoRequest request) {
        return resguardoPdf.generar(request);
    }

    /** Vale de entrada del equipo que se recibe en el taller. */
    public byte[] generarEntradaEquipoPdf(EntradaEquipoRequest request) {
        return entradaEquipoPdf.generar(request);
    }

    /**
     * Reporte de actividades del periodo en PDF.
     *
     * <p>Recorre relaciones perezosas del ticket, de modo que quien lo invoca
     * debe mantener abierta la sesion de Hibernate.</p>
     */
    public byte[] generarReporteActividadesPdf(LocalDate fechaInicio, LocalDate fechaFin,
                                               List<Ticket> tickets,
                                               List<ActividadExtra> actividades) {
        return reporteActividades.generarPdf(fechaInicio, fechaFin, tickets, actividades);
    }

    /** El mismo reporte en hoja de calculo. Comparte la exigencia de sesion abierta. */
    public byte[] generarReporteActividadesExcel(List<Ticket> tickets,
                                                 List<ActividadExtra> actividades) {
        return reporteActividades.generarExcel(tickets, actividades);
    }
}
