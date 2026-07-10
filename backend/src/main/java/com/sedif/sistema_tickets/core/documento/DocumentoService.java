package com.sedif.sistema_tickets.core.documento;

// Importaciones de iText (PDF) explícitas para evitar choques
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
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

// Importaciones de Spring y utilidades de Java
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Importaciones de Apache POI (Excel) explícitas
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class DocumentoService {

    // Fuentes globales
    private final Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private final Font fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private final Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

    // =========================================================================================
    // 1. GENERACIÓN DE MEMORÁNDUM
    // =========================================================================================
    public byte[] generarMemorandum(MemorandumRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            agregarEncabezadoInstitucional(document, "MEMORÁNDUM INTERNO");

            Paragraph pPara = new Paragraph();
            pPara.add(new Chunk("PARA: ", fuenteNegrita));
            pPara.add(new Chunk(request.para(), fuenteNormal));
            document.add(pPara);

            Paragraph pDe = new Paragraph();
            pDe.add(new Chunk("DE: ", fuenteNegrita));
            pDe.add(new Chunk(request.de(), fuenteNormal)); 
            document.add(pDe);

            Paragraph pAsunto = new Paragraph();
            pAsunto.add(new Chunk("ASUNTO: ", fuenteNegrita));
            pAsunto.add(new Chunk(request.asunto(), fuenteNormal)); 
            pAsunto.setSpacingAfter(10);
            document.add(pAsunto);

            Paragraph cuerpo = new Paragraph(request.cuerpo(), fuenteNormal);
            cuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(cuerpo);
            
            document.add(new Paragraph("\n\n"));

            Paragraph firma = new Paragraph("___________________________________\n" + request.de(), fuenteNormal);
            firma.setAlignment(Element.ALIGN_CENTER);
            document.add(firma);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el Memorandum", e);
        }
    }

    // =========================================================================================
    // 2. GENERACIÓN DE REQUISICIÓN
    // =========================================================================================
    public byte[] generarRequisicion(RequisicionRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            agregarEncabezadoInstitucional(document, "REQUISICIÓN DE MATERIAL");

            Paragraph pArea = new Paragraph();
            pArea.add(new Chunk("ÁREA SOLICITANTE: ", fuenteNegrita));
            pArea.add(new Chunk(request.areaSolicitante(), fuenteNormal)); 
            document.add(pArea);

            Paragraph pFechaReq = new Paragraph();
            pFechaReq.add(new Chunk("FECHA REQUERIDA: ", fuenteNegrita));
            pFechaReq.add(new Chunk(request.fechaRequerida(), fuenteNormal));
            document.add(pFechaReq);

            Paragraph pJust = new Paragraph();
            pJust.add(new Chunk("JUSTIFICACIÓN: ", fuenteNegrita));
            pJust.add(new Chunk(request.justificacion(), fuenteNormal));
            pJust.setSpacingAfter(20);
            document.add(pJust);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2f, 6.5f});

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

            for (ArticuloDTO articulo : request.listaArticulos()) {
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
            document.add(new Paragraph("\n\n\n"));

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

    private void agregarEncabezadoInstitucional(Document document, String titulo) throws DocumentException {
        Font fontInstitucion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);

        Paragraph pInstitucion = new Paragraph("SISTEMA PARA EL DESARROLLO INTEGRAL DE LA FAMILIA\nDEL ESTADO DE PUEBLA", fontInstitucion);
        pInstitucion.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph pTitulo = new Paragraph("\n" + titulo.toUpperCase(), fontTitulo);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingAfter(20f); 

        document.add(pInstitucion);
        document.add(pTitulo);
    }

    // =========================================================================================
    // 3. GENERACIÓN DE DICTAMEN TÉCNICO (CON EL NUEVO ENCABEZADO OFICIAL)
    // =========================================================================================
    public byte[] generarDictamenTecnicoPdf(DictamenRequest request) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        // Márgenes laterales de 36 (0.5 pulgada) para dar espacio al diseño
        Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
        Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7, Color.BLACK);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

        // --- 1. LOGO GRANDE Y CENTRADO ---
        PdfPTable tableLogo = new PdfPTable(1);
        tableLogo.setWidthPercentage(100);
        PdfPCell cellLogo = new PdfPCell();
        cellLogo.setBorder(Rectangle.NO_BORDER);
        cellLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellLogo.setPaddingBottom(15); 
        try {
            com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(getClass().getClassLoader().getResource("logo-puebla.png"));
            // Escalamos para que sea notablemente más grande
            logo.scaleToFit(220, 110); 
            logo.setAlignment(Element.ALIGN_CENTER);
            cellLogo.addElement(logo);
        } catch (Exception e) {
            System.err.println("Error cargando logo: " + e.getMessage());
        }
        tableLogo.addCell(cellLogo);
        document.add(tableLogo);

        // --- 2. TEXTOS A LA DERECHA (BLOQUE COMPACTO) ---
        PdfPTable tableTextos = new PdfPTable(2);
        tableTextos.setWidthPercentage(100);
        // Proporción 60% vacío a la izquierda, 40% texto a la derecha
        tableTextos.setWidths(new float[]{1.5f, 1f}); 

        PdfPCell cellVacia = new PdfPCell();
        cellVacia.setBorder(Rectangle.NO_BORDER);
        tableTextos.addCell(cellVacia);

        PdfPCell cellTexto = new PdfPCell();
        cellTexto.setBorder(Rectangle.NO_BORDER);
        cellTexto.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        Paragraph textos = new Paragraph();
        textos.setAlignment(Element.ALIGN_RIGHT);
        textos.setLeading(9f); // Ajuste de interlineado para que se vea compacto
        
        textos.add(new Chunk("UNIDAD DE PLANEACIÓN, ADMINISTRACIÓN Y FINANZAS\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        textos.add(new Chunk("DIRECCIÓN DE RECURSOS MATERIALES, SERVICIOS GENERALES, ARCHIVO Y SOPORTE TÉCNICO\n", FontFactory.getFont(FontFactory.HELVETICA, 7)));
        textos.add(new Chunk("DEPARTAMENTO DE SOPORTE TÉCNICO\n", FontFactory.getFont(FontFactory.HELVETICA, 7)));
        textos.add(new Chunk("Heroica Puebla de Zaragoza, a " + (request.fecha() != null ? request.fecha() : ""), FontFactory.getFont(FontFactory.HELVETICA, 7)));
        
        cellTexto.addElement(textos);
        tableTextos.addCell(cellTexto);
        document.add(tableTextos);

        document.add(new Paragraph("\n"));
        
        // --- TÍTULO ---
        Paragraph titulo = new Paragraph("DICTAMEN TÉCNICO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(15);
        document.add(titulo);

        // --- RESTO DEL DOCUMENTO ---
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
        agregarLineaDatoUsuario(tUsuario, "DIRECCIÓN:", request.direccionUsuario(), fontNormal);
        agregarLineaDatoUsuario(tUsuario, "DEPARTAMENTO:", request.departamentoUsuario(), fontNormal);
        agregarLineaDatoUsuario(tUsuario, "NOMBRE DE USUARIO:", request.nombreUsuario(), fontNormal);
        agregarLineaDatoUsuario(tUsuario, "TELEFONO / EXT:", request.telefonoUsuario(), fontNormal);
        agregarLineaDatoUsuario(tUsuario, "TIPO DE REPORTE:", request.tipoReporte(), fontNormal);
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
            PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", fontNormal));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tEquipo.addCell(cell);
        }
        document.add(tEquipo);

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
        document.add(new Paragraph("\n\n")); 

        PdfPTable tFirmas = new PdfPTable(3);
        tFirmas.setWidthPercentage(100);
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

// Métodos auxiliares necesarios en tu clase
private void agregarLineaDatoUsuario(PdfPTable table, String label, String value, Font font) {
    PdfPCell cellLabel = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7)));
    cellLabel.setBorder(Rectangle.NO_BORDER);
    table.addCell(cellLabel);
    
    PdfPCell cellValue = new PdfPCell(new Phrase(value != null ? value : "", font));
    cellValue.setBorder(Rectangle.NO_BORDER);
    table.addCell(cellValue);
}

private String formatearNombreFirma(String nombre) {
    return (nombre != null && !nombre.isEmpty()) ? nombre.toUpperCase() : "___________________________";
}

    // =========================================================================================
    // 4. GENERACIÓN DE REPORTE DE ACTIVIDADES PDF
    // =========================================================================================
    public byte[] generarReporteActividadesPdf(LocalDate fechaInicio, LocalDate fechaFin, 
        List<com.sedif.sistema_tickets.core.ticket.Ticket> tickets, 
        List<com.sedif.sistema_tickets.core.actividad.ActividadExtra> actividades) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.BLACK);
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Color colorGuinda = new Color(128, 26, 54); 

            Paragraph pDir = new Paragraph("Dirección de Recursos Materiales, Servicios Generales, Archivo y Soporte Técnico", fontTitulo);
            pDir.setAlignment(Element.ALIGN_CENTER);
            document.add(pDir);

            Paragraph pDep = new Paragraph("Departamento de Soporte Técnico", fontTitulo);
            pDep.setAlignment(Element.ALIGN_CENTER);
            document.add(pDep);

            Paragraph pRep = new Paragraph("Reporte de Actividades", fontSubtitulo);
            pRep.setAlignment(Element.ALIGN_CENTER);
            pRep.setSpacingAfter(10);
            document.add(pRep);

            Paragraph pFechas = new Paragraph("Fecha del reporte: Del " + fechaInicio + " al " + fechaFin, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, colorGuinda));
            pFechas.setSpacingAfter(5);
            document.add(pFechas);

            PdfPTable table = new PdfPTable(new float[]{1f, 1.5f, 2f, 2f, 1.5f, 2f, 1.5f, 2.5f});
            table.setWidthPercentage(100);

            String[] cabeceras = {
                "N° del Plan de Trabajo", "N° de Memorándum / Oficio / Orden de Servicio",
                "Área Solicitante", "Actividad Solicitada", "Fecha de Asignación",
                "Situación Actual", "Responsable", "Actividad de Solución"
            };

            for (String cab : cabeceras) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontCabecera));
                cell.setBackgroundColor(colorGuinda);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (com.sedif.sistema_tickets.core.ticket.Ticket t : tickets) {
                table.addCell(crearCelda(t.getPlanTrabajoClave() != null ? String.valueOf(t.getPlanTrabajoClave()) : "", fontNormal, Element.ALIGN_CENTER));
                table.addCell(crearCelda(String.valueOf(t.getId()), fontNormal, Element.ALIGN_CENTER));
                String areaTicket = "";
                if (t.getUsuarioArea() != null && t.getUsuarioArea().getArea() != null) {
                    areaTicket = t.getUsuarioArea().getArea().getNombre();
                } else if (t.getSolicitanteNombre() != null) {
                    areaTicket = t.getSolicitanteNombre();
                }
                table.addCell(crearCelda(areaTicket, fontNormal, Element.ALIGN_LEFT));
                table.addCell(crearCelda(t.getTitulo() != null ? t.getTitulo() : "", fontNormal, Element.ALIGN_LEFT));
                table.addCell(crearCelda(t.getFechaFin() != null ? t.getFechaFin().format(formatter) : "", fontNormal, Element.ALIGN_CENTER));
                String estatus = (t.getEstatus() != null && t.getEstatus().getNombre() != null) ? t.getEstatus().getNombre() : "CERRADO";
                table.addCell(crearCelda(estatus, fontNormal, Element.ALIGN_LEFT));
                String responsableTicket = (t.getUsuarioSoporte() != null && t.getUsuarioSoporte().getNombre() != null) 
                                     ? t.getUsuarioSoporte().getNombre() : "Sin Asignar";
                table.addCell(crearCelda(responsableTicket, fontNormal, Element.ALIGN_CENTER));
                table.addCell(crearCelda(t.getJustificacion() != null ? t.getJustificacion() : "", fontNormal, Element.ALIGN_LEFT));
            }

            for (com.sedif.sistema_tickets.core.actividad.ActividadExtra a : actividades) {
                table.addCell(crearCelda(a.getPlanTrabajoClave() != null ? String.valueOf(a.getPlanTrabajoClave()) : "", fontNormal, Element.ALIGN_CENTER));
                table.addCell(crearCelda("N/A", fontNormal, Element.ALIGN_CENTER));
                String area = "";
                if (a.getUsuario() != null && a.getUsuario().getArea() != null) {
                    area = a.getUsuario().getArea().getNombre();
                }
                table.addCell(crearCelda(area, fontNormal, Element.ALIGN_LEFT));
                table.addCell(crearCelda(a.getActividadSolicitada() != null ? a.getActividadSolicitada() : "", fontNormal, Element.ALIGN_LEFT));
                table.addCell(crearCelda(a.getFechaActividad() != null ? a.getFechaActividad().format(formatter) : "", fontNormal, Element.ALIGN_CENTER));
                table.addCell(crearCelda(a.getSituacionActual() != null ? a.getSituacionActual() : "", fontNormal, Element.ALIGN_LEFT));
                String responsable = (a.getUsuario() != null && a.getUsuario().getNombre() != null) 
                                     ? a.getUsuario().getNombre() : "Sin Asignar";
                table.addCell(crearCelda(responsable, fontNormal, Element.ALIGN_CENTER));
                table.addCell(crearCelda(a.getJustificacion() != null ? a.getJustificacion() : "", fontNormal, Element.ALIGN_LEFT));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte de actividades", e);
        }
    }

    private PdfPCell crearCelda(String texto, Font fuente, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    // =========================================================================================
    // 5. GENERACIÓN DE REPORTE DE ACTIVIDADES EXCEL
    // =========================================================================================
    public byte[] generarReporteActividadesExcel(List<com.sedif.sistema_tickets.core.ticket.Ticket> tickets, 
        List<com.sedif.sistema_tickets.core.actividad.ActividadExtra> actividades) {
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
            String[] cabeceras = {
                "N° del Plan de Trabajo", "N° de Memorándum", "Área Solicitante", 
                "Actividad Solicitada", "Fecha de Asignación", "Situación Actual", 
                "Responsable", "Actividad de Solución"
            };

            for (int i = 0; i < cabeceras.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cabeceras[i]);
                cell.setCellStyle(styleCabecera);
                sheet.setColumnWidth(i, 6000); 
            }

            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (com.sedif.sistema_tickets.core.ticket.Ticket t : tickets) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getPlanTrabajoClave() != null ? String.valueOf(t.getPlanTrabajoClave()) : "");
                row.createCell(1).setCellValue(String.valueOf(t.getId()));
                String areaTicket = "";
                if (t.getUsuarioArea() != null && t.getUsuarioArea().getArea() != null) {
                    areaTicket = t.getUsuarioArea().getArea().getNombre();
                } else if (t.getSolicitanteNombre() != null) {
                    areaTicket = t.getSolicitanteNombre();
                }
                row.createCell(2).setCellValue(areaTicket);
                row.createCell(3).setCellValue(t.getTitulo() != null ? t.getTitulo() : "");
                row.createCell(4).setCellValue(t.getFechaFin() != null ? t.getFechaFin().format(formatter) : "");
                String estatus = (t.getEstatus() != null && t.getEstatus().getNombre() != null) ? t.getEstatus().getNombre() : "CERRADO";
                row.createCell(5).setCellValue(estatus);
                String responsableTicket = (t.getUsuarioSoporte() != null && t.getUsuarioSoporte().getNombre() != null) 
                                     ? t.getUsuarioSoporte().getNombre() : "Sin Asignar";
                row.createCell(6).setCellValue(responsableTicket);
                row.createCell(7).setCellValue(t.getJustificacion() != null ? t.getJustificacion() : "");
            }

            for (com.sedif.sistema_tickets.core.actividad.ActividadExtra a : actividades) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getPlanTrabajoClave() != null ? String.valueOf(a.getPlanTrabajoClave()) : "");
                row.createCell(1).setCellValue("N/A"); 
                String area = "";
                if (a.getUsuario() != null && a.getUsuario().getArea() != null) {
                    area = a.getUsuario().getArea().getNombre();
                }
                row.createCell(2).setCellValue(area);
                row.createCell(3).setCellValue(a.getActividadSolicitada() != null ? a.getActividadSolicitada() : "");
                row.createCell(4).setCellValue(a.getFechaActividad() != null ? a.getFechaActividad().format(formatter) : "");
                row.createCell(5).setCellValue(a.getSituacionActual() != null ? a.getSituacionActual() : "");
                String responsable = (a.getUsuario() != null && a.getUsuario().getNombre() != null) 
                                     ? a.getUsuario().getNombre() : "Sin Asignar";
                row.createCell(6).setCellValue(responsable);
                row.createCell(7).setCellValue(a.getJustificacion() != null ? a.getJustificacion() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }
}