package com.sedif.sistema_tickets.core.documento.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sedif.sistema_tickets.core.documento.EntradaEquipoDetalleDTO;
import com.sedif.sistema_tickets.core.documento.EntradaEquipoRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * Composicion del vale de entrada de equipo en PDF.
 *
 * <p>Documenta el traslado: fecha y folio, ubicacion de origen y destino, y una
 * tabla con todos los equipos que ampara el mismo formato. Cierra con las
 * firmas de entrega y recepcion.</p>
 */
@Component
public class EntradaEquipoPdf {

    public byte[] generar(EntradaEquipoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            ElementosPdf.agregarEncabezadoInstitucional(document, "ENTRADA DE EQUIPO");

            PdfPTable tInfo = new PdfPTable(2);
            tInfo.setWidthPercentage(100);
            tInfo.addCell(ElementosPdf.celdaSimple("FECHA: " + request.getFecha(), Element.ALIGN_LEFT));
            tInfo.addCell(ElementosPdf.celdaSimple("FOLIO TICKET: " + request.getFolioTicket(), Element.ALIGN_RIGHT));
            document.add(tInfo);
            document.add(new Paragraph("\n"));

            PdfPTable tUbicaciones = new PdfPTable(2);
            tUbicaciones.setWidthPercentage(100);
            tUbicaciones.addCell(ElementosPdf.celdaSimple(
                    "UBICACIÓN ACTUAL: " + request.getDepartamentoActual() + " / " + request.getDireccionActual(),
                    Element.ALIGN_LEFT));
            tUbicaciones.addCell(ElementosPdf.celdaSimple(
                    "UBICACIÓN DESTINO: " + request.getResguardoDestino() + " / " + request.getDireccionDestino(),
                    Element.ALIGN_LEFT));
            document.add(tUbicaciones);
            document.add(new Paragraph("\n"));

            PdfPTable tEquipos = new PdfPTable(new float[]{1f, 3f, 2f, 2f, 2f, 2f});
            tEquipos.setWidthPercentage(100);
            String[] cabeceras = {"CVE", "DESCRIPCIÓN", "MARCA", "MODELO", "SERIE", "CANT."};
            for (String cab : cabeceras) {
                tEquipos.addCell(ElementosPdf.celdaCabecera(cab));
            }
            for (EntradaEquipoDetalleDTO eq : request.getEquipos()) {
                tEquipos.addCell(ElementosPdf.celdaNormal(eq.getCve()));
                tEquipos.addCell(ElementosPdf.celdaNormal(eq.getDescripcion()));
                tEquipos.addCell(ElementosPdf.celdaNormal(eq.getMarca()));
                tEquipos.addCell(ElementosPdf.celdaNormal(eq.getModelo()));
                tEquipos.addCell(ElementosPdf.celdaNormal(eq.getSerie()));
                tEquipos.addCell(ElementosPdf.celdaNormal(eq.getCantidad()));
            }
            document.add(tEquipos);

            ElementosPdf.agregarFirmas(document, request.getEntregadoPor(), request.getRecibidoPor());

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el formato de entrada de equipo.", e);
        }
    }
}
