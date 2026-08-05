package com.sedif.sistema_tickets.core.documento;

// Importaciones de iText (PDF)
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
import com.sedif.sistema_tickets.core.resguardo.ResguardoRequest;

// Importaciones de Spring y utilidades de Java
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Importaciones de Apache POI (Excel)
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;

@Service
@Slf4j
public class DocumentoService {

    // Fuentes globales
    private final Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private final Font fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private final Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
    
    // CORRECCIÓN: Se quitó 'final' y se agregó @Autowired para inyectar correctamente la BD
    @Autowired
    private UsuarioRepository usuarioRepository;

    private void agregarEncabezadoInstitucional(Document document, String titulo) throws DocumentException {
        Font fontInstitucion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Font fontTituloDoc = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);

        Paragraph pInstitucion = new Paragraph("SISTEMA PARA EL DESARROLLO INTEGRAL DE LA FAMILIA\nDEL ESTADO DE PUEBLA", fontInstitucion);
        pInstitucion.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph pTitulo = new Paragraph("\n" + titulo.toUpperCase(), fontTituloDoc);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingAfter(20f); 

        document.add(pInstitucion);
        document.add(pTitulo);
    }

    // =========================================================================================
    // 1. GENERACIÓN DE DICTAMEN TÉCNICO
    // =========================================================================================
    public byte[] generarDictamenTecnicoPdf(DictamenRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7, Color.BLACK);
            Font fontNorm = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            PdfPTable tableLogo = new PdfPTable(1);
            tableLogo.setWidthPercentage(100);
            PdfPCell cellLogo = new PdfPCell();
            cellLogo.setBorder(Rectangle.NO_BORDER);
            cellLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellLogo.setPaddingBottom(15); 
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(getClass().getClassLoader().getResource("logo-puebla.png"));
                logo.scaleToFit(220, 110); 
                logo.setAlignment(Element.ALIGN_CENTER);
                cellLogo.addElement(logo);
            } catch (Exception e) {
                log.warn("No se pudo cargar el logotipo institucional en el documento.", e);
            }
            tableLogo.addCell(cellLogo);
            document.add(tableLogo);

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
            agregarLineaDatoUsuario(tUsuario, "DIRECCIÓN:", request.direccionUsuario(), fontNorm);
            agregarLineaDatoUsuario(tUsuario, "DEPARTAMENTO:", request.departamentoUsuario(), fontNorm);
            agregarLineaDatoUsuario(tUsuario, "NOMBRE DE USUARIO:", request.nombreUsuario(), fontNorm);
            agregarLineaDatoUsuario(tUsuario, "TELEFONO / EXT:", request.telefonoUsuario(), fontNorm);
            agregarLineaDatoUsuario(tUsuario, "TIPO DE REPORTE:", request.tipoReporte(), fontNorm);
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
            throw new IllegalStateException("No se pudo generar el dictamen tecnico. Revise que los datos esten completos.", e);
        }
    }

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
    // 2. GENERACIÓN DE REPORTE DE ACTIVIDADES PDF
    // =========================================================================================
    public byte[] generarReporteActividadesPdf(LocalDate fechaInicio, LocalDate fechaFin, 
        List<com.sedif.sistema_tickets.core.ticket.Ticket> tickets, 
        List<com.sedif.sistema_tickets.core.actividad.ActividadExtra> actividades) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTituloDoc = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.BLACK);
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontNorm = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Color colorGuinda = new Color(128, 26, 54); 

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
                table.addCell(crearCeldaAuxiliar(t.getPlanTrabajoClave() != null ? String.valueOf(t.getPlanTrabajoClave()) : "", fontNorm, Element.ALIGN_CENTER));
                table.addCell(crearCeldaAuxiliar(String.valueOf(t.getId()), fontNorm, Element.ALIGN_CENTER));
                String areaTicket = "";
                if (t.getUsuarioArea() != null && t.getUsuarioArea().getArea() != null) {
                    areaTicket = t.getUsuarioArea().getArea().getNombre();
                } else if (t.getSolicitanteNombre() != null) {
                    areaTicket = t.getSolicitanteNombre();
                }
                table.addCell(crearCeldaAuxiliar(areaTicket, fontNorm, Element.ALIGN_LEFT));
                table.addCell(crearCeldaAuxiliar(t.getTitulo() != null ? t.getTitulo() : "", fontNorm, Element.ALIGN_LEFT));
                table.addCell(crearCeldaAuxiliar(t.getFechaFin() != null ? t.getFechaFin().format(formatter) : "", fontNorm, Element.ALIGN_CENTER));
                String estatus = t.getEstado() != null ? t.getEstado().getEtiqueta() : "Cerrado";
                table.addCell(crearCeldaAuxiliar(estatus, fontNorm, Element.ALIGN_LEFT));
                String responsableTicket = (t.getUsuarioSoporte() != null && t.getUsuarioSoporte().getNombre() != null) 
                                     ? t.getUsuarioSoporte().getNombre() : "Sin Asignar";
                table.addCell(crearCeldaAuxiliar(responsableTicket, fontNorm, Element.ALIGN_CENTER));
                table.addCell(crearCeldaAuxiliar(t.getJustificacion() != null ? t.getJustificacion() : "", fontNorm, Element.ALIGN_LEFT));
            }

            for (com.sedif.sistema_tickets.core.actividad.ActividadExtra a : actividades) {
                table.addCell(crearCeldaAuxiliar(a.getPlanTrabajoClave() != null ? String.valueOf(a.getPlanTrabajoClave()) : "", fontNorm, Element.ALIGN_CENTER));
                table.addCell(crearCeldaAuxiliar("N/A", fontNorm, Element.ALIGN_CENTER));
                String area = "";
                if (a.getUsuario() != null && a.getUsuario().getArea() != null) {
                    area = a.getUsuario().getArea().getNombre();
                }
                table.addCell(crearCeldaAuxiliar(area, fontNorm, Element.ALIGN_LEFT));
                table.addCell(crearCeldaAuxiliar(a.getActividadSolicitada() != null ? a.getActividadSolicitada() : "", fontNorm, Element.ALIGN_LEFT));
                table.addCell(crearCeldaAuxiliar(a.getFechaActividad() != null ? a.getFechaActividad().format(formatter) : "", fontNorm, Element.ALIGN_CENTER));
                table.addCell(crearCeldaAuxiliar(a.getSituacionActual() != null ? a.getSituacionActual() : "", fontNorm, Element.ALIGN_LEFT));
                String responsable = (a.getUsuario() != null && a.getUsuario().getNombre() != null) 
                                     ? a.getUsuario().getNombre() : "Sin Asignar";
                table.addCell(crearCeldaAuxiliar(responsable, fontNorm, Element.ALIGN_CENTER));
                table.addCell(crearCeldaAuxiliar(a.getJustificacion() != null ? a.getJustificacion() : "", fontNorm, Element.ALIGN_LEFT));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte de actividades.", e);
        }
    }

    private PdfPCell crearCeldaAuxiliar(String texto, Font fuente, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", fuente));
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    // =========================================================================================
    // 3. GENERACIÓN DE REPORTE DE ACTIVIDADES EXCEL
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
                String estatus = t.getEstado() != null ? t.getEstado().getEtiqueta() : "Cerrado";
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
            // La causa se registra antes de envolverla: el handler global solo
            // devuelve el mensaje, asi que sin esta traza el fallo real de POI
            // quedaba invisible tanto en el log como en la respuesta.
            log.error("Fallo al generar el reporte de actividades en Excel.", e);
            throw new IllegalStateException("No se pudo generar el archivo de Excel.", e);
        }
    }
    
    // =========================================================================================
    // 4. GENERACIÓN DE RESGUARDO (MÉTODO COMPLETO DEFINITIVO CON NOMBRE DE PERFIL REAL)
    // =========================================================================================
    public byte[] generarResguardo(ResguardoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7, Color.BLACK);
            Font fontNorm = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            // 1. SECCIÓN: LOGO OFICIAL
            PdfPTable tableLogo = new PdfPTable(1);
            tableLogo.setWidthPercentage(100);
            PdfPCell cellLogo = new PdfPCell();
            cellLogo.setBorder(Rectangle.NO_BORDER);
            cellLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellLogo.setPaddingBottom(15); 
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(getClass().getClassLoader().getResource("logo-puebla.png"));
                logo.scaleToFit(220, 110); 
                logo.setAlignment(Element.ALIGN_CENTER);
                cellLogo.addElement(logo);
            } catch (Exception e) {
                log.warn("No se pudo cargar el logotipo institucional en el documento.", e);
            }
            tableLogo.addCell(cellLogo);
            document.add(tableLogo);

            // 2. SECCIÓN: ENCABEZADO DERECHO (ÁREA E INSTITUCIÓN)
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
            
            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            textos.add(new Chunk("UNIDAD DE PLANEACIÓN, ADMINISTRACIÓN Y FINANZAS\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
            textos.add(new Chunk("DIRECCIÓN DE RECURSOS MATERIALES, SERVICIOS GENERALES, ARCHIVO Y SOPORTE TÉCNICO\n", FontFactory.getFont(FontFactory.HELVETICA, 7)));
            textos.add(new Chunk("DEPARTAMENTO DE SOPORTE TÉCNICO\n", FontFactory.getFont(FontFactory.HELVETICA, 7)));
            textos.add(new Chunk("Heroica Puebla de Zaragoza, a " + fechaActual, FontFactory.getFont(FontFactory.HELVETICA, 7)));
            
            cellTexto.addElement(textos);
            tableTextos.addCell(cellTexto);
            document.add(tableTextos);
            document.add(new Paragraph("\n"));
            
            // TÍTULO CENTRAL
            Paragraph titulo = new Paragraph("RESGUARDO DE EQUIPO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(15);
            document.add(titulo);

            // 3. SECCIÓN: FECHA Y NO. DE EMPLEADO
            PdfPTable tFechaFolio = new PdfPTable(2);
            tFechaFolio.setWidthPercentage(100);
            PdfPCell cFecha = new PdfPCell(new Phrase("FECHA: " + fechaActual, fontBold));
            cFecha.setBorder(Rectangle.BOX);
            
            PdfPCell cEmpleado = new PdfPCell(new Phrase("No. EMPLEADO: " + (request.solicitanteNumero() != null ? request.solicitanteNumero() : "N/A"), fontBold));
            cEmpleado.setBorder(Rectangle.BOX);
            cEmpleado.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            tFechaFolio.addCell(cFecha);
            tFechaFolio.addCell(cEmpleado);
            document.add(tFechaFolio);
            document.add(new Paragraph("\n"));

            // 4. SECCIÓN: DATOS DEL USUARIO (CON PREFIJO "C. ")
            document.add(new Phrase("DATOS DEL USUARIO\n", fontBold));
            PdfPTable tUsuario = new PdfPTable(new float[]{3f, 7f});
            tUsuario.setWidthPercentage(100);
            
            java.util.function.BiConsumer<String, String> agregarFilaUsu = (lbl, val) -> {
                PdfPCell cLbl = new PdfPCell(new Phrase(lbl, fontBold)); cLbl.setBorder(Rectangle.NO_BORDER);
                PdfPCell cVal = new PdfPCell(new Phrase(val != null ? val : "N/A", fontNorm)); cVal.setBorder(Rectangle.NO_BORDER);
                tUsuario.addCell(cLbl); tUsuario.addCell(cVal);
            };

            agregarFilaUsu.accept("NOMBRE DE USUARIO:", formatearNombre(request.solicitanteNombre()));
            agregarFilaUsu.accept("DEPARTAMENTO:", request.departamento());
            agregarFilaUsu.accept("TELÉFONO / EXT:", request.telefono());
            document.add(tUsuario);
            document.add(new Paragraph("\n"));

            // 5. SECCIÓN: TABLA DE CARACTERÍSTICAS DEL EQUIPO
            PdfPTable tEquipo = new PdfPTable(new float[]{2f, 2f, 2f, 1.5f, 1.5f, 1.5f});
            tEquipo.setWidthPercentage(100);
            String[] cabecerasEquipo = {"EQUIPO", "No. SERIE", "No. INVENTARIO", "CONDICIONES", "TIPO VIG.", "CANTIDAD"};
            for (String cab : cabecerasEquipo) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontBold));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            
            String[] valoresEquipo = {
                request.equipoNombre(), request.numeroSerie(), request.numeroInventario(), 
                request.condiciones(), request.duracionTipo(), String.valueOf(request.duracionCantidad())
            };
            for (String val : valoresEquipo) {
                PdfPCell cell = new PdfPCell(new Phrase(val != null && !val.equals("null") ? val : "N/A", fontNorm));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            document.add(tEquipo);

            // 6. SECCIÓN: OBSERVACIONES Y DECLARATORIA
            PdfPTable tTextos = new PdfPTable(1);
            tTextos.setWidthPercentage(100);
            Paragraph cuerpoTextos = new Paragraph();
            cuerpoTextos.add(new Chunk("ACCESORIOS Y OBSERVACIONES:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.accesorios() != null ? request.accesorios() : "SIN ACCESORIOS ADICIONALES") + "\n\n", fontNorm));
            
            cuerpoTextos.add(new Chunk("DECLARATORIA DE RESGUARDO:\n", fontBold));
            cuerpoTextos.add(new Chunk("Por medio de la presente, me comprometo a resguardar y hacer buen uso del equipo descrito anteriormente, el cual me es asignado para el desempeño de mis funciones. En caso de robo, extravío o daño derivado del mal uso, me haré responsable de la reparación o reposición del mismo conforme a la normativa aplicable.\n", fontNorm));
            
            PdfPCell cellTextos = new PdfPCell(cuerpoTextos);
            cellTextos.setPadding(10);
            cellTextos.setMinimumHeight(180f); 
            tTextos.addCell(cellTextos);
            document.add(tTextos);
            document.add(new Paragraph("\n")); 

            // =====================================================================
            // 7. SECCIÓN DEL APARTADO DE FIRMAS DINÁMICO
            // =====================================================================
            PdfPTable tFirmas = new PdfPTable(2);
            tFirmas.setWidthPercentage(100);
            tFirmas.setSpacingBefore(15f);
            
            // --- COLUMNA IZQUIERDA: ENTREGA (Nombre real completo recuperado desde la BD) ---
            PdfPCell cEntrega = new PdfPCell();
            cEntrega.setBorder(Rectangle.NO_BORDER);
            cEntrega.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            Paragraph pEntrega = new Paragraph();
            pEntrega.setAlignment(Element.ALIGN_CENTER);
            pEntrega.setLeading(13f);
            pEntrega.add(new Chunk("ENTREGA:\n(Nombre y Firma)\n\n\n\n\n", fontBoldItalic)); 
            pEntrega.add(new Chunk("___________________________\n", fontBoldItalic));
            
            String operador = "SOPORTE TÉCNICO";
            try {
                org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                
                if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                    String correoLogueado = auth.getName();
                    
                    // Buscamos al usuario en la BD usando el correo de la sesión
                    var usuarioOpt = usuarioRepository.findByCorreo(correoLogueado);
                    if (usuarioOpt.isPresent()) {
                        com.sedif.sistema_tickets.core.usuarios.Usuario u = usuarioOpt.get();
                        
                        // Concatenamos el Nombre Completo tal cual aparece al lado de tu perfil
                        StringBuilder nombreCompleto = new StringBuilder(u.getNombre());
                        if (u.getApellidoPaterno() != null && !u.getApellidoPaterno().trim().isEmpty()) {
                            nombreCompleto.append(" ").append(u.getApellidoPaterno());
                        }
                        if (u.getApellidoMaterno() != null && !u.getApellidoMaterno().trim().isEmpty()) {
                            nombreCompleto.append(" ").append(u.getApellidoMaterno());
                        }
                        operador = nombreCompleto.toString();
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo determinar el perfil del usuario para el documento.", e);
            }

            pEntrega.add(new Chunk(formatearNombre(operador), fontBoldItalic));
            cEntrega.addElement(pEntrega);
            tFirmas.addCell(cEntrega);
            
            // --- COLUMNA DERECHA: RECIBE (El Servidor Público / Usuario) ---
            PdfPCell cRecibe = new PdfPCell();
            cRecibe.setBorder(Rectangle.NO_BORDER);
            cRecibe.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            Paragraph pRecibe = new Paragraph();
            pRecibe.setAlignment(Element.ALIGN_CENTER);
            pRecibe.setLeading(13f);
            pRecibe.add(new Chunk("RECIBE (USUARIO):\n(Nombre y Firma)\n\n\n\n\n", fontBoldItalic)); 
            pRecibe.add(new Chunk("___________________________\n", fontBoldItalic));
            pRecibe.add(new Chunk(formatearNombre(request.solicitanteNombre()), fontBoldItalic));
            
            cRecibe.addElement(pRecibe);
            tFirmas.addCell(cRecibe);

            document.add(tFirmas);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la responsiva de resguardo.", e);
        }
    }

    private String formatearNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "N/A";
        String n = nombre.trim();
        return n.toUpperCase().startsWith("C. ") ? n.toUpperCase() : "C. " + n.toUpperCase();
    }

    // =========================================================================================
    // 5. GENERACIÓN DE ENTRADA DE EQUIPO
    // =========================================================================================
    public byte[] generarEntradaEquipoPdf(EntradaEquipoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            agregarEncabezadoInstitucional(document, "ENTRADA DE EQUIPO");

            PdfPTable tInfo = new PdfPTable(2);
            tInfo.setWidthPercentage(100);
            tInfo.addCell(crearCeldaSimple("FECHA: " + request.getFecha(), Element.ALIGN_LEFT));
            tInfo.addCell(crearCeldaSimple("FOLIO TICKET: " + request.getFolioTicket(), Element.ALIGN_RIGHT));
            document.add(tInfo);
            document.add(new Paragraph("\n"));

            PdfPTable tUbicaciones = new PdfPTable(2);
            tUbicaciones.setWidthPercentage(100);
            tUbicaciones.addCell(crearCeldaSimple("UBICACIÓN ACTUAL: " + request.getDepartamentoActual() + " / " + request.getDireccionActual(), Element.ALIGN_LEFT));
            tUbicaciones.addCell(crearCeldaSimple("UBICACIÓN DESTINO: " + request.getResguardoDestino() + " / " + request.getDireccionDestino(), Element.ALIGN_LEFT));
            document.add(tUbicaciones);
            document.add(new Paragraph("\n"));

            PdfPTable tEquipos = new PdfPTable(new float[]{1f, 3f, 2f, 2f, 2f, 2f});
            tEquipos.setWidthPercentage(100);
            String[] cabeceras = {"CVE", "DESCRIPCIÓN", "MARCA", "MODELO", "SERIE", "CANT."};
            for (String cab : cabeceras) {
                tEquipos.addCell(crearCeldaCabecera(cab));
            }
            for (EntradaEquipoDetalleDTO eq : request.getEquipos()) {
                tEquipos.addCell(crearCeldaNormal(eq.getCve()));
                tEquipos.addCell(crearCeldaNormal(eq.getDescripcion()));
                tEquipos.addCell(crearCeldaNormal(eq.getMarca()));
                tEquipos.addCell(crearCeldaNormal(eq.getModelo()));
                tEquipos.addCell(crearCeldaNormal(eq.getSerie()));
                tEquipos.addCell(crearCeldaNormal(eq.getCantidad()));
            }
            document.add(tEquipos);

            agregarFirmas(document, request.getEntregadoPor(), request.getRecibidoPor());

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el formato de entrada de equipo.", e);
        }
    }


    // =========================================================================================
    // MÉTODOS AUXILIARES GLOBALES
    // =========================================================================================
    private PdfPCell crearCeldaCabecera(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell crearCeldaNormal(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell crearCeldaSimple(String texto, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private void agregarFirmas(Document doc, String nombre1, String nombre2) throws Exception {
        PdfPTable tFirmas = new PdfPTable(2);
        tFirmas.setWidthPercentage(80);
        tFirmas.setSpacingBefore(30);
        tFirmas.addCell(crearCeldaSimple("ENTREGA:\n\n\n__________________\n" + nombre1, Element.ALIGN_CENTER));
        tFirmas.addCell(crearCeldaSimple("RECIBE:\n\n\n__________________\n" + nombre2, Element.ALIGN_CENTER));
        doc.add(tFirmas);
    }
    
}