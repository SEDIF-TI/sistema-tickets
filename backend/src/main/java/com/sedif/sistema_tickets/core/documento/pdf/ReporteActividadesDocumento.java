package com.sedif.sistema_tickets.core.documento.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sedif.sistema_tickets.core.actividad.ActividadExtra;
import com.sedif.sistema_tickets.core.ticket.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reporte de actividades del periodo, en PDF y en hoja de calculo.
 *
 * <p>Los dos formatos vuelcan las mismas dos fuentes de trabajo del area —los
 * tickets cerrados y las actividades registradas a mano— sobre las mismas ocho
 * columnas del formato oficial. En los tickets el numero de oficio es el folio;
 * en las actividades sueltas no aplica.</p>
 *
 * <p>Ambos recorren relaciones perezosas del ticket (el area del usuario, el
 * tecnico asignado), de modo que quien los invoca debe mantener abierta la
 * sesion de Hibernate.</p>
 */
@Slf4j
@Component
public class ReporteActividadesDocumento {

    /** Guinda institucional, para la cabecera de la tabla. */
    private static final Color COLOR_GUINDA = new Color(128, 26, 54);

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] CABECERAS_PDF = {
        "N° del Plan de Trabajo", "N° de Memorándum / Oficio / Orden de Servicio",
        "Área Solicitante", "Actividad Solicitada", "Fecha de Asignación",
        "Situación Actual", "Responsable", "Actividad de Solución"
    };

    private static final String[] CABECERAS_EXCEL = {
        "N° del Plan de Trabajo", "N° de Memorándum", "Área Solicitante",
        "Actividad Solicitada", "Fecha de Asignación", "Situación Actual",
        "Responsable", "Actividad de Solución"
    };

    /**
     * Compone el reporte en PDF.
     *
     * <p>La pagina va apaisada porque de otro modo esas ocho columnas no caben
     * con un ancho legible.</p>
     */
    public byte[] generarPdf(LocalDate fechaInicio, LocalDate fechaFin,
                             List<Ticket> tickets, List<ActividadExtra> actividades) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTituloDoc = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.BLACK);
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontNorm = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

            Paragraph pDir = new Paragraph("Dirección de Recursos Materiales, Servicios Generales, Archivo y Soporte Técnico", fontTituloDoc);
            pDir.setAlignment(Element.ALIGN_CENTER);
            document.add(pDir);

            Paragraph pDep = new Paragraph("Departamento de Soporte Técnico", fontTituloDoc);
            pDep.setAlignment(Element.ALIGN_CENTER);
            document.add(pDep);

            Paragraph pRep = new Paragraph("Reporte de Actividades", fontSubtitulo);
            pRep.setAlignment(Element.ALIGN_CENTER);
            pRep.setSpacingAfter(10);
            document.add(pRep);

            Paragraph pFechas = new Paragraph("Fecha del reporte: Del " + fechaInicio + " al " + fechaFin,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_GUINDA));
            pFechas.setSpacingAfter(5);
            document.add(pFechas);

            PdfPTable table = new PdfPTable(new float[]{1f, 1.5f, 2f, 2f, 1.5f, 2f, 1.5f, 2.5f});
            table.setWidthPercentage(100);

            for (String cab : CABECERAS_PDF) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontCabecera));
                cell.setBackgroundColor(COLOR_GUINDA);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Ticket t : tickets) {
                table.addCell(celda(t.getPlanTrabajoClave() != null ? String.valueOf(t.getPlanTrabajoClave()) : "", fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda(String.valueOf(t.getId()), fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda(areaDe(t), fontNorm, Element.ALIGN_LEFT));
                table.addCell(celda(t.getTitulo() != null ? t.getTitulo() : "", fontNorm, Element.ALIGN_LEFT));
                table.addCell(celda(t.getFechaFin() != null ? t.getFechaFin().format(FORMATO_FECHA) : "", fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda(estadoDe(t), fontNorm, Element.ALIGN_LEFT));
                table.addCell(celda(responsableDe(t), fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda(t.getJustificacion() != null ? t.getJustificacion() : "", fontNorm, Element.ALIGN_LEFT));
            }

            for (ActividadExtra a : actividades) {
                table.addCell(celda(a.getPlanTrabajoClave() != null ? String.valueOf(a.getPlanTrabajoClave()) : "", fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda("N/A", fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda(areaDe(a), fontNorm, Element.ALIGN_LEFT));
                table.addCell(celda(a.getActividadSolicitada() != null ? a.getActividadSolicitada() : "", fontNorm, Element.ALIGN_LEFT));
                table.addCell(celda(a.getFechaActividad() != null ? a.getFechaActividad().format(FORMATO_FECHA) : "", fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda(a.getSituacionActual() != null ? a.getSituacionActual() : "", fontNorm, Element.ALIGN_LEFT));
                table.addCell(celda(responsableDe(a), fontNorm, Element.ALIGN_CENTER));
                table.addCell(celda(a.getJustificacion() != null ? a.getJustificacion() : "", fontNorm, Element.ALIGN_LEFT));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte de actividades.", e);
        }
    }

    /**
     * Compone el mismo reporte como libro de Excel.
     *
     * <p>A diferencia del PDF, aqui el destinatario puede filtrar y sumar por
     * su cuenta.</p>
     */
    public byte[] generarExcel(List<Ticket> tickets, List<ActividadExtra> actividades) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte de Actividades");

            CellStyle styleCabecera = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fontCabecera = workbook.createFont();
            fontCabecera.setBold(true);
            fontCabecera.setColor(IndexedColors.WHITE.getIndex());
            styleCabecera.setFont(fontCabecera);
            styleCabecera.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            styleCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleCabecera.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < CABECERAS_EXCEL.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(CABECERAS_EXCEL[i]);
                cell.setCellStyle(styleCabecera);
                sheet.setColumnWidth(i, 6000);
            }

            int rowIdx = 1;

            for (Ticket t : tickets) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getPlanTrabajoClave() != null ? String.valueOf(t.getPlanTrabajoClave()) : "");
                row.createCell(1).setCellValue(String.valueOf(t.getId()));
                row.createCell(2).setCellValue(areaDe(t));
                row.createCell(3).setCellValue(t.getTitulo() != null ? t.getTitulo() : "");
                row.createCell(4).setCellValue(t.getFechaFin() != null ? t.getFechaFin().format(FORMATO_FECHA) : "");
                row.createCell(5).setCellValue(estadoDe(t));
                row.createCell(6).setCellValue(responsableDe(t));
                row.createCell(7).setCellValue(t.getJustificacion() != null ? t.getJustificacion() : "");
            }

            for (ActividadExtra a : actividades) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getPlanTrabajoClave() != null ? String.valueOf(a.getPlanTrabajoClave()) : "");
                row.createCell(1).setCellValue("N/A");
                row.createCell(2).setCellValue(areaDe(a));
                row.createCell(3).setCellValue(a.getActividadSolicitada() != null ? a.getActividadSolicitada() : "");
                row.createCell(4).setCellValue(a.getFechaActividad() != null ? a.getFechaActividad().format(FORMATO_FECHA) : "");
                row.createCell(5).setCellValue(a.getSituacionActual() != null ? a.getSituacionActual() : "");
                row.createCell(6).setCellValue(responsableDe(a));
                row.createCell(7).setCellValue(a.getJustificacion() != null ? a.getJustificacion() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            // La causa se registra al envolverla: el handler global solo
            // traslada el mensaje, de modo que sin esta traza el fallo real de
            // POI no constaria en ninguna parte.
            log.error("Fallo al generar el reporte de actividades en Excel.", e);
            throw new IllegalStateException("No se pudo generar el archivo de Excel.", e);
        }
    }

    // ------------------------------------------------------------ apoyos

    /** Celda del reporte: centrada en vertical y tolerante a valores nulos. */
    private PdfPCell celda(String texto, Font fuente, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", fuente));
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    /**
     * Area solicitante del ticket. Cuando el usuario no tiene area asignada se
     * recurre al nombre del solicitante, que al menos identifica de donde sale
     * el reporte.
     */
    private String areaDe(Ticket t) {
        if (t.getUsuarioArea() != null && t.getUsuarioArea().getArea() != null) {
            return t.getUsuarioArea().getArea().getNombre();
        }
        return t.getSolicitanteNombre() != null ? t.getSolicitanteNombre() : "";
    }

    private String areaDe(ActividadExtra a) {
        if (a.getUsuario() != null && a.getUsuario().getArea() != null) {
            return a.getUsuario().getArea().getNombre();
        }
        return "";
    }

    /** Los tickets del reporte estan cerrados; el valor por defecto lo refleja. */
    private String estadoDe(Ticket t) {
        return t.getEstado() != null ? t.getEstado().getEtiqueta() : "Cerrado";
    }

    private String responsableDe(Ticket t) {
        return (t.getUsuarioSoporte() != null && t.getUsuarioSoporte().getNombre() != null)
                ? t.getUsuarioSoporte().getNombre()
                : "Sin Asignar";
    }

    private String responsableDe(ActividadExtra a) {
        return (a.getUsuario() != null && a.getUsuario().getNombre() != null)
                ? a.getUsuario().getNombre()
                : "Sin Asignar";
    }
}
