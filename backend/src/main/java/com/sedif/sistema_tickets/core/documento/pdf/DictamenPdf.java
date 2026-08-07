package com.sedif.sistema_tickets.core.documento.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sedif.sistema_tickets.core.documento.DictamenRequest;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * Composicion del dictamen tecnico en PDF.
 *
 * <p>Sigue el formato oficial en tamano carta: logotipo, encabezado del
 * departamento alineado a la derecha, recuadros de fecha y folio, los datos del
 * usuario que reporto, una fila con la identificacion del equipo y el bloque de
 * analisis con la falla y el diagnostico. Cierra con los tres espacios de
 * firma, donde quien recibe es siempre el usuario del reporte.</p>
 *
 * <p>El bloque de textos reserva una altura minima para que el documento
 * conserve su proporcion aunque el diagnostico sea breve.</p>
 */
@Component
public class DictamenPdf {

    public byte[] generar(DictamenRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7, Color.BLACK);
            Font fontNorm = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            ElementosPdf.agregarLogotipo(document, 220, 110);

            // El encabezado ocupa la mitad derecha: la primera celda queda
            // vacia para empujarlo hacia ese lado.
            PdfPTable tableTextos = new PdfPTable(2);
            tableTextos.setWidthPercentage(100);
            tableTextos.setWidths(new float[]{1.5f, 1f});

            PdfPCell cellVacia = new PdfPCell();
            cellVacia.setBorder(Rectangle.NO_BORDER);
            tableTextos.addCell(cellVacia);

            PdfPCell cellTexto = new PdfPCell();
            cellTexto.setBorder(Rectangle.NO_BORDER);
            cellTexto.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph textos = new Paragraph();
            textos.setAlignment(Element.ALIGN_RIGHT);
            textos.setLeading(9f);

            textos.add(new Chunk("UNIDAD DE PLANEACIÓN, ADMINISTRACIÓN Y FINANZAS\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
            textos.add(new Chunk("DIRECCIÓN DE RECURSOS MATERIALES, SERVICIOS GENERALES, ARCHIVO Y SOPORTE TÉCNICO\n", FontFactory.getFont(FontFactory.HELVETICA, 7)));
            textos.add(new Chunk("DEPARTAMENTO DE SOPORTE TÉCNICO\n", FontFactory.getFont(FontFactory.HELVETICA, 7)));
            textos.add(new Chunk("Heroica Puebla de Zaragoza, a " + (request.fecha() != null ? request.fecha() : ""), FontFactory.getFont(FontFactory.HELVETICA, 7)));

            cellTexto.addElement(textos);
            tableTextos.addCell(cellTexto);
            document.add(tableTextos);
            document.add(new Paragraph("\n"));

            Paragraph titulo = new Paragraph("DICTAMEN TÉCNICO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(15);
            document.add(titulo);

            PdfPTable tFechaFolio = new PdfPTable(2);
            tFechaFolio.setWidthPercentage(100);
            PdfPCell cFecha = new PdfPCell(new Phrase("FECHA: " + (request.fecha() != null ? request.fecha() : "_________________"), fontBold));
            cFecha.setBorder(Rectangle.BOX);
            String folioStr = String.format("%04d", request.folioTicket() != null ? request.folioTicket() : 0);
            PdfPCell cFolio = new PdfPCell(new Phrase("No. FOLIO: " + folioStr, fontBold));
            cFolio.setBorder(Rectangle.BOX);
            cFolio.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tFechaFolio.addCell(cFecha);
            tFechaFolio.addCell(cFolio);
            document.add(tFechaFolio);
            document.add(new Paragraph("\n"));

            document.add(new Phrase("DATOS DEL USUARIO\n", fontBold));
            PdfPTable tUsuario = new PdfPTable(new float[]{3f, 7f});
            tUsuario.setWidthPercentage(100);
            ElementosPdf.agregarLineaDato(tUsuario, "DIRECCIÓN:", request.direccionUsuario(), fontNorm);
            ElementosPdf.agregarLineaDato(tUsuario, "DEPARTAMENTO:", request.departamentoUsuario(), fontNorm);
            ElementosPdf.agregarLineaDato(tUsuario, "NOMBRE DE USUARIO:", request.nombreUsuario(), fontNorm);
            ElementosPdf.agregarLineaDato(tUsuario, "TELEFONO / EXT:", request.telefonoUsuario(), fontNorm);
            ElementosPdf.agregarLineaDato(tUsuario, "TIPO DE REPORTE:", request.tipoReporte(), fontNorm);
            document.add(tUsuario);
            document.add(new Paragraph("\n"));

            PdfPTable tEquipo = new PdfPTable(new float[]{1f, 3f, 1.5f, 1.5f, 2f, 2f, 0.8f});
            tEquipo.setWidthPercentage(100);
            String[] cabecerasEquipo = {"CVE.", "DESCRIPCIÓN", "MARCA", "MODELO", "No. SERIE", "NO. RESGUARDO", "NO."};
            for (String cab : cabecerasEquipo) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontBold));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            String[] valoresEquipo = {request.cve(), request.descripcionEquipo(), request.marca(), request.modelo(), request.serie(), request.noResguardo(), "1"};
            for (String val : valoresEquipo) {
                PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", fontNorm));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            document.add(tEquipo);

            PdfPTable tTextos = new PdfPTable(1);
            tTextos.setWidthPercentage(100);
            Paragraph cuerpoTextos = new Paragraph();
            cuerpoTextos.add(new Chunk("DESCRIPCIÓN DE LA FALLA:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.fallaReportada() != null ? request.fallaReportada() : "") + "\n\n", fontNorm));
            cuerpoTextos.add(new Chunk("DIAGNÓSTICO TÉCNICO:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.diagnostico() != null ? request.diagnostico() : ""), fontNorm));
            PdfPCell cellTextos = new PdfPCell(cuerpoTextos);
            cellTextos.setPadding(10);
            cellTextos.setMinimumHeight(200f);
            tTextos.addCell(cellTextos);
            document.add(tTextos);
            document.add(new Paragraph("\n\n"));

            PdfPTable tFirmas = new PdfPTable(3);
            tFirmas.setWidthPercentage(100);
            String fRealizo = ElementosPdf.nombreDeFirma(request.realizadoPor());
            String fReviso = ElementosPdf.nombreDeFirma(request.revisadoPor());
            String fRecibio = ElementosPdf.nombreDeFirma(request.nombreUsuario());
            String[] firmas = {fRealizo, fReviso, fRecibio};
            String[] titulosFirmas = {"REALIZADO POR:", "REVISADO Y AUTORIZADO POR:", "RECIBIDO POR:"};
            for (int i = 0; i < 3; i++) {
                PdfPCell cFirma = new PdfPCell(new Phrase(titulosFirmas[i] + "\n(Nombre y Firma)\n\n\n___________________________\n" + firmas[i], fontBoldItalic));
                cFirma.setBorder(Rectangle.NO_BORDER);
                cFirma.setHorizontalAlignment(Element.ALIGN_CENTER);
                tFirmas.addCell(cFirma);
            }
            document.add(tFirmas);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo generar el dictamen tecnico. Revise que los datos esten completos.", e);
        }
    }
}
