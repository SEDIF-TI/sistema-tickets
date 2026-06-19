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

    public byte[] generarMemorandum(MemorandumRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Definición de Tipografías
            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Font fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

            // =========================================================
            // 2. ENCABEZADO INSTITUCIONAL (Tabla invisible de 2 columnas)
            // =========================================================
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1f, 2f}); // La columna derecha es más ancha

            // Celda 1: Logotipo
            PdfPCell celdaLogo = new PdfPCell();
            celdaLogo.setBorder(Rectangle.NO_BORDER);
            
            try {
                // Usamos ClassPathResource de Spring, que es 100% seguro para Docker y archivos .jar
                org.springframework.core.io.ClassPathResource imgFile = new org.springframework.core.io.ClassPathResource("logo.png");
                
                if (imgFile.exists()) {
                    // Leemos la imagen como un flujo de bytes, evitando problemas de rutas físicas
                    byte[] bytesImagen = org.springframework.util.StreamUtils.copyToByteArray(imgFile.getInputStream());
                    Image logo = Image.getInstance(bytesImagen);
                    logo.scaleToFit(120, 120); // Ajustamos el tamaño máximo
                    celdaLogo.addElement(logo);
                } else {
                    celdaLogo.addElement(new Paragraph("[Logo faltante]", fuenteNormal));
                }
            } catch (Exception e) {
                System.out.println("Error al cargar el logo: " + e.getMessage());
                celdaLogo.addElement(new Paragraph("[Error de imagen]", fuenteNormal));
            }
            headerTable.addCell(celdaLogo);

            // Celda 2: Datos de Emisión (Alineados a la derecha)
            PdfPCell celdaDatos = new PdfPCell();
            celdaDatos.setBorder(Rectangle.NO_BORDER);
            celdaDatos.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"));
            Paragraph pFecha = new Paragraph("Puebla, Pue. a " + fechaActual, fuenteNormal);
            pFecha.setAlignment(Element.ALIGN_RIGHT);
            
            Paragraph pTitulo = new Paragraph("\nMEMORÁNDUM INTERNO", fuenteTitulo);
            pTitulo.setAlignment(Element.ALIGN_RIGHT);

            celdaDatos.addElement(pFecha);
            celdaDatos.addElement(pTitulo);
            headerTable.addCell(celdaDatos);

            document.add(headerTable);
            document.add(new Paragraph("\n")); // Espacio extra

            // =========================================================
            // 3. CUERPO DEL MEMORÁNDUM
            // =========================================================
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

            // Línea separadora
            LineSeparator linea = new LineSeparator();
            linea.setLineColor(Color.DARK_GRAY);
            document.add(linea);

            // Texto Principal
            Paragraph cuerpo = new Paragraph(request.cuerpoMensaje(), fuenteNormal);
            cuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            cuerpo.setSpacingBefore(20);
            cuerpo.setSpacingAfter(60);
            document.add(cuerpo);

            // =========================================================
            // 4. SECCIÓN DE FIRMAS
            // =========================================================
            Paragraph firma = new Paragraph("___________________________________\n" + request.remitente(), fuenteNormal);
            firma.setAlignment(Element.ALIGN_CENTER);
            document.add(firma);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el Memorandum", e);
        }
    }
}