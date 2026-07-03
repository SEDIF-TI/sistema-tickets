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
            pPara.add(new Chunk(request.para(), fuenteNormal)); // Actualizado
            document.add(pPara);

            Paragraph pDe = new Paragraph();
            pDe.add(new Chunk("DE: ", fuenteNegrita));
            pDe.add(new Chunk(request.de(), fuenteNormal)); // Actualizado
            document.add(pDe);

            Paragraph pAsunto = new Paragraph();
            pAsunto.add(new Chunk("ASUNTO: ", fuenteNegrita));
            pAsunto.add(new Chunk(request.asunto(), fuenteNormal)); // Actualizado
            pAsunto.setSpacingAfter(10);
            document.add(pAsunto);

            // ... (Línea separadora)

            Paragraph cuerpo = new Paragraph(request.cuerpo(), fuenteNormal); // Actualizado
            cuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            // ... (Resto del código)

            // 3. Firma
            Paragraph firma = new Paragraph("___________________________________\n" + request.de(), fuenteNormal); // Actualizado
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
            pArea.add(new Chunk(request.areaSolicitante(), fuenteNormal)); // Usamos lo que manda React
            document.add(pArea);

            // ---> NUEVO: Agregamos la fecha requerida que viene de React <---
            Paragraph pFechaReq = new Paragraph();
            pFechaReq.add(new Chunk("FECHA REQUERIDA: ", fuenteNegrita));
            pFechaReq.add(new Chunk(request.fechaRequerida(), fuenteNormal));
            document.add(pFechaReq);

            Paragraph pJust = new Paragraph();
            pJust.add(new Chunk("JUSTIFICACIÓN: ", fuenteNegrita));
            pJust.add(new Chunk(request.justificacion(), fuenteNormal)); // Ya estaba bien
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
            for (ArticuloDTO articulo : request.listaArticulos()) { // Actualizado el nombre de la lista
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
            
            PdfPCell celdaFirmaSol = new PdfPCell(new Phrase("___________________________________\n" + request.areaSolicitante() + "\nSolicita", fuenteNormal));
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
    private void agregarEncabezadoInstitucional(Document document, String titulo) throws DocumentException {
        // --- AQUÍ IRÍAN LOS LOGOS MÁS ADELANTE ---
        // // logo.scaleToFit(120, 120);
        // ------------------------------------------

        // Fuente para la institución
        Font fontInstitucion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        // Fuente para el título del documento
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);

        Paragraph pInstitucion = new Paragraph("SISTEMA PARA EL DESARROLLO INTEGRAL DE LA FAMILIA\nDEL ESTADO DE PUEBLA", fontInstitucion);
        pInstitucion.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph pTitulo = new Paragraph("\n" + titulo.toUpperCase(), fontTitulo);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingAfter(20f); // Espacio antes de los datos del usuario

        document.add(pInstitucion);
        document.add(pTitulo);
    }

    // =========================================================
    // MÉTODO PARA EL DICTAMEN TÉCNICO (VISTA LIMPIA Y ORDENADA)
    // =========================================================
    public byte[] generarDictamenTecnicoPdf(DictamenRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36); 
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7, Color.BLACK);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            // 1. ENCABEZADO Y LOGOS
            agregarEncabezadoInstitucional(document, "DICTAMEN TÉCNICO DE EQUIPO DE CÓMPUTO");

            // 2. FECHA Y FOLIO
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

            // 3. DATOS DEL USUARIO (Subidos antes de la tabla del equipo)
            document.add(new Phrase("DATOS DEL USUARIO\n", fontBold));
            PdfPTable tUsuario = new PdfPTable(new float[]{3f, 7f});
            tUsuario.setWidthPercentage(100);
            
            agregarLineaDatoUsuario(tUsuario, "DIRECCIÓN:", request.direccionUsuario(), fontNormal);
            agregarLineaDatoUsuario(tUsuario, "DEPARTAMENTO:", request.departamentoUsuario(), fontNormal);
            agregarLineaDatoUsuario(tUsuario, "NOMBRE DE USUARIO:", request.nombreUsuario(), fontNormal);
            agregarLineaDatoUsuario(tUsuario, "TELEFONO / EXT:", request.telefonoUsuario(), fontNormal);
            agregarLineaDatoUsuario(tUsuario, "TIPO DE REPORTE:", request.tipoReporte(), fontNormal);
            document.add(tUsuario);
            document.add(new Paragraph("\n"));

            // 4. TABLA DEL EQUIPO
            PdfPTable tEquipo = new PdfPTable(new float[]{1f, 3f, 1.5f, 1.5f, 2f, 2f, 0.8f});
            tEquipo.setWidthPercentage(100);
            
            String[] cabecerasEquipo = {"CVE.", "DESCRIPCIÓN", "MARCA", "MODELO", "No. SERIE", "NO. RESGUARDO", "NO."};
            for (String cab : cabecerasEquipo) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontBold));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            
            String[] valoresEquipo = {
                request.cve(), request.descripcionEquipo(), request.marca(), 
                request.modelo(), request.serie(), request.noResguardo(), "1"
            };
            for (String val : valoresEquipo) {
                PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", fontNormal));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            document.add(tEquipo);

            // 5. BLOQUE CENTRAL DE TEXTO (Falla, Diagnóstico, Conclusión)
            PdfPTable tTextos = new PdfPTable(1);
            tTextos.setWidthPercentage(100);
            
            Paragraph cuerpoTextos = new Paragraph();
            cuerpoTextos.add(new Chunk("DESCRIPCIÓN DE LA FALLA:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.fallaReportada() != null ? request.fallaReportada() : "") + "\n\n", fontNormal));
            
            cuerpoTextos.add(new Chunk("DIAGNÓSTICO TÉCNICO:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.diagnostico() != null ? request.diagnostico() : "") + "\n\n", fontNormal));
            
            cuerpoTextos.add(new Chunk("HALLAZGOS:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.hallazgos() != null ? request.hallazgos() : "") + "\n\n", fontNormal));
            
            cuerpoTextos.add(new Chunk("CONCLUSIÓN:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.conclusion() != null ? request.conclusion() : ""), fontNormal));

            PdfPCell cellTextos = new PdfPCell(cuerpoTextos);
            cellTextos.setPadding(10);
            cellTextos.setMinimumHeight(200f); 
            tTextos.addCell(cellTextos);
            document.add(tTextos);
            document.add(new Paragraph("\n\n")); // Espacio para separar de las firmas

            // 6. FIRMAS (3 Columnas automáticas con formato C. NOMBRE)
            PdfPTable tFirmas = new PdfPTable(3);
            tFirmas.setWidthPercentage(100);
            
            // Función interna para formatear el nombre
            String fRealizo = formatearNombreFirma(request.realizadoPor());
            String fReviso = formatearNombreFirma(request.revisadoPor());
            String fRecibio = formatearNombreFirma(request.nombreUsuario());
            
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
            throw new RuntimeException("Error al generar el formato oficial de Dictamen", e);
        }
    }

    // Método auxiliar para dibujar la línea inferior en los datos de usuario
    private void agregarLineaDatoUsuario(PdfPTable table, String etiqueta, String valor, Font font) {
        PdfPCell cEtiq = new PdfPCell(new Phrase(etiqueta, font));
        cEtiq.setBorder(Rectangle.NO_BORDER);
        
        PdfPCell cVal = new PdfPCell(new Phrase(valor != null ? valor : "", font));
        cVal.setBorder(Rectangle.BOTTOM); // Dibuja la línea inferior para simular el llenado
        cVal.setBorderWidthBottom(0.5f);
        
        table.addCell(cEtiq);
        table.addCell(cVal);
    }

    private String formatearNombreFirma(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        
        String nombreLimpio = nombre.toUpperCase().trim();
        
        // Verificamos si ya empieza con "C." o "C " para no duplicarlo
        if (nombreLimpio.startsWith("C.") || nombreLimpio.startsWith("C ")) {
            return nombreLimpio;
        }
        
        return "C. " + nombreLimpio;
    }

    // =========================================================
    // MÉTODO PARA MANTENIMIENTO PREVENTIVO
    // =========================================================
    public byte[] generarMantenimientoPdf(MantenimientoPreventivoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Usamos formato horizontal (rotate) para que quepan las 10 columnas
            Document document = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36); 
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            // 1. TÍTULOS PRINCIPALES (Sustituyendo el encabezado general para igualar tu imagen)
            Paragraph pDir = new Paragraph("DIRECCIÓN DE ADMINISTRACIÓN Y FINANZAS\nDEPARTAMENTO DE SOPORTE TÉCNICO\nLISTADO DE EQUIPOS DE CÓMPUTO AL QUE SE LE DIO MANTENIMIENTO PREVENTIVO\n(" + (request.departamento() != null ? request.departamento().toUpperCase() : "") + ")", fontTitulo);
            pDir.setAlignment(Element.ALIGN_CENTER);
            document.add(pDir);
            
            // Rango de Fechas alineado a la derecha
            Paragraph pFechas = new Paragraph((request.fechaInicio() != null ? request.fechaInicio() : "") + " - " + (request.fechaFin() != null ? request.fechaFin() : ""), fontNormal);
            pFechas.setAlignment(Element.ALIGN_RIGHT);
            pFechas.setSpacingAfter(10f);
            document.add(pFechas);

            // 2. TABLA PRINCIPAL (10 Columnas)
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            // Definimos anchos proporcionales para que unas columnas sean más anchas que otras
            table.setWidths(new float[]{1f, 3.5f, 4.5f, 2.5f, 2.5f, 3f, 3f, 2f, 2.5f, 3f});

            // Encabezados de la tabla (Fondo Gris)
            String[] cabeceras = {"No.", "ÁREA", "NOMBRE DE USUARIO RESPONSABLE", "TIPO DE CPU", "MARCA", "MODELO", "NO. DE SERIE", "MEMORIA RAM", "CAPACIDAD DISCO DURO", "NO. DE INVENTARIO DE EQUIPO"};
            for (String cab : cabeceras) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontCabecera));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            // Filas (Llenado dinámico iterando la lista de React)
            int index = 1;
            if (request.equipos() != null) {
                for (EquipoMantenimientoDTO equipo : request.equipos()) {
                    // El número autoincrementable se inyecta con la variable 'index'
                    table.addCell(crearCeldaTabla(String.valueOf(index++), fontNormal, Element.ALIGN_CENTER));
                    table.addCell(crearCeldaTabla(equipo.area(), fontNormal, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.usuarioResponsable(), fontNormal, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.tipoCpu(), fontNormal, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.marca(), fontNormal, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.modelo(), fontNormal, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.numeroSerie(), fontNormal, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.memoriaRam(), fontNormal, Element.ALIGN_CENTER));
                    table.addCell(crearCeldaTabla(equipo.capacidadDisco(), fontNormal, Element.ALIGN_CENTER));
                    table.addCell(crearCeldaTabla(equipo.numeroInventario(), fontNormal, Element.ALIGN_CENTER));
                }
            }
            document.add(table);
            document.add(new Paragraph("\n\n\n\n")); // Espaciado para las firmas

            // 3. FIRMAS (2 Columnas sin bordes)
            PdfPTable tFirmas = new PdfPTable(2);
            tFirmas.setWidthPercentage(100);

            // Firma Izquierda (El que realizó)
            String textoRealizo = "REALIZÓ EL SERVICIO\nSOPORTE TÉCNICO\n\n\n\n___________________________________\n" 
                                + (request.nombreTecnico() != null ? request.nombreTecnico().toUpperCase() : "") 
                                + "\nSOPORTE TÉCNICO";
            PdfPCell cRealizo = new PdfPCell(new Phrase(textoRealizo, fontCabecera));
            cRealizo.setBorder(Rectangle.NO_BORDER);
            cRealizo.setHorizontalAlignment(Element.ALIGN_CENTER);

            // Firma Derecha (Departamento del Usuario)
            String textoConformidad = "FIRMA DE CONFORMIDAD\nPOR PARTE DEL USUARIO\n\n\n\n___________________________________\n" 
                                    + (request.departamento() != null ? request.departamento().toUpperCase() : "");
            PdfPCell cConformidad = new PdfPCell(new Phrase(textoConformidad, fontCabecera));
            cConformidad.setBorder(Rectangle.NO_BORDER);
            cConformidad.setHorizontalAlignment(Element.ALIGN_CENTER);

            tFirmas.addCell(cRealizo);
            tFirmas.addCell(cConformidad);
            document.add(tFirmas);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el documento de Mantenimiento Preventivo", e);
        }
    }

    // Método auxiliar para evitar repetir código al crear celdas de la tabla principal
    private PdfPCell crearCeldaTabla(String texto, Font font, int alineacion) {
        // Todo lo que llega nulo se limpia, y todo se convierte a mayúsculas para cumplir la regla
        String textoLimpio = texto != null ? texto.toUpperCase().trim() : "";
        PdfPCell cell = new PdfPCell(new Phrase(textoLimpio, font));
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        return cell;
    }
}