package com.sedif.sistema_tickets.core.documento;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DocumentoService {

    // Fuentes globales
    private final Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private final Font fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private final Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

    public byte[] generarMemorandum(MemorandumRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Encabezado Reutilizable
            agregarEncabezadoInstitucional(document, "MEMORÁNDUM INTERNO");

            // 2. Cuerpo del Memorándum
            Paragraph pPara = new Paragraph();
            pPara.add(new Chunk("PARA: ", fuenteNegrita));
            pPara.add(new Chunk(request.destinatario(), fuenteNormal));
            document.add(pPara);

            Paragraph pDe = new Paragraph();
            pDe.add(new Chunk("DE: ", fuenteNegrita));
            pDe.add(new Chunk(request.remitente(), fuenteNormal));
            document.add(pDe);

            Paragraph pAsunto = new Paragraph();
            pAsunto.add(new Chunk("ASUNTO: ", fuenteNegrita));
            pAsunto.add(new Chunk(request.asunto(), fuenteNormal));
            pAsunto.setSpacingAfter(10);
            document.add(pAsunto);

            LineSeparator linea = new LineSeparator();
            linea.setLineColor(Color.DARK_GRAY);
            document.add(linea);

            Paragraph cuerpo = new Paragraph(request.cuerpoMensaje(), fuenteNormal);
            cuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            cuerpo.setSpacingBefore(20);
            cuerpo.setSpacingAfter(60);
            document.add(cuerpo);

            // 3. Firma
            Paragraph firma = new Paragraph("___________________________________\n" + request.remitente(), fuenteNormal);
            firma.setAlignment(Element.ALIGN_CENTER);
            document.add(firma);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el Memorandum", e);
        }
    }

    public byte[] generarRequisicion(RequisicionRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Encabezado Reutilizable
            agregarEncabezadoInstitucional(document, "REQUISICIÓN DE MATERIAL");

            // 2. Datos Generales
            Paragraph pArea = new Paragraph();
            pArea.add(new Chunk("ÁREA SOLICITANTE: ", fuenteNegrita));
            pArea.add(new Chunk("Soporte Técnico", fuenteNormal));
            document.add(pArea);

            Paragraph pSol = new Paragraph();
            pSol.add(new Chunk("NOMBRE DEL SOLICITANTE: ", fuenteNegrita));
            pSol.add(new Chunk(request.solicitante(), fuenteNormal));
            document.add(pSol);

            Paragraph pJust = new Paragraph();
            pJust.add(new Chunk("JUSTIFICACIÓN: ", fuenteNegrita));
            pJust.add(new Chunk(request.justificacion(), fuenteNormal));
            pJust.setSpacingAfter(20);
            document.add(pJust);

            // 3. TABLA DE ARTÍCULOS DINÁMICA
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2f, 6.5f}); // Proporción de las columnas

            // Cabeceras de la tabla
            PdfPCell h1 = new PdfPCell(new Phrase("CANTIDAD", fuenteNegrita));
            h1.setBackgroundColor(Color.LIGHT_GRAY);
            h1.setHorizontalAlignment(Element.ALIGN_CENTER);
            h1.setPadding(5);
            table.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("UNIDAD", fuenteNegrita));
            h2.setBackgroundColor(Color.LIGHT_GRAY);
            h2.setHorizontalAlignment(Element.ALIGN_CENTER);
            h2.setPadding(5);
            table.addCell(h2);

            PdfPCell h3 = new PdfPCell(new Phrase("DESCRIPCIÓN DEL ARTÍCULO", fuenteNegrita));
            h3.setBackgroundColor(Color.LIGHT_GRAY);
            h3.setHorizontalAlignment(Element.ALIGN_CENTER);
            h3.setPadding(5);
            table.addCell(h3);

            // Rellenar filas dinámicamente con los datos de React
            for (ArticuloDTO articulo : request.articulos()) {
                PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(articulo.cantidad()), fuenteNormal));
                c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                c1.setPadding(5);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(articulo.unidad(), fuenteNormal));
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                c2.setPadding(5);
                table.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase(articulo.descripcion(), fuenteNormal));
                c3.setPadding(5);
                table.addCell(c3);
            }
            
            document.add(table);
            document.add(new Paragraph("\n\n\n")); // Espaciado para firmas

            // 4. Firmas (Tabla invisible para firmas alineadas)
            PdfPTable tableFirmas = new PdfPTable(2);
            tableFirmas.setWidthPercentage(100);
            
            PdfPCell celdaFirmaSol = new PdfPCell(new Phrase("___________________________________\n" + request.solicitante() + "\nSolicita", fuenteNormal));
            celdaFirmaSol.setBorder(Rectangle.NO_BORDER);
            celdaFirmaSol.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            PdfPCell celdaFirmaAut = new PdfPCell(new Phrase("___________________________________\nNombre y Firma\nAutoriza", fuenteNormal));
            celdaFirmaAut.setBorder(Rectangle.NO_BORDER);
            celdaFirmaAut.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            tableFirmas.addCell(celdaFirmaSol);
            tableFirmas.addCell(celdaFirmaAut);
            
            document.add(tableFirmas);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar la Requisicion", e);
        }
    }

    // =========================================================
    // MÉTODO PRIVADO: ENCABEZADO REUTILIZABLE
    // =========================================================
    private void agregarEncabezadoInstitucional(Document document, String tituloDocumento) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1f, 2f});

        PdfPCell celdaLogo = new PdfPCell();
        celdaLogo.setBorder(Rectangle.NO_BORDER);
        try {
            org.springframework.core.io.ClassPathResource imgFile = new org.springframework.core.io.ClassPathResource("logo.png");
            if (imgFile.exists()) {
                byte[] bytesImagen = org.springframework.util.StreamUtils.copyToByteArray(imgFile.getInputStream());
                Image logo = Image.getInstance(bytesImagen);
                logo.scaleToFit(120, 120);
                celdaLogo.addElement(logo);
            } else {
                celdaLogo.addElement(new Paragraph("[Logo faltante]", fuenteNormal));
            }
        } catch (Exception e) {
            System.out.println("Error al cargar el logo: " + e.getMessage());
            celdaLogo.addElement(new Paragraph("[Error de imagen]", fuenteNormal));
        }
        headerTable.addCell(celdaLogo);

        PdfPCell celdaDatos = new PdfPCell();
        celdaDatos.setBorder(Rectangle.NO_BORDER);
        celdaDatos.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"));
        Paragraph pFecha = new Paragraph("Puebla, Pue. a " + fechaActual, fuenteNormal);
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        
        Paragraph pTitulo = new Paragraph("\n" + tituloDocumento, fuenteTitulo);
        pTitulo.setAlignment(Element.ALIGN_RIGHT);

        celdaDatos.addElement(pFecha);
        celdaDatos.addElement(pTitulo);
        headerTable.addCell(celdaDatos);

        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }
}