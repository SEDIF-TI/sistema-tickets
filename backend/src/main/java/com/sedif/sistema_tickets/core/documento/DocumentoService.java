package com.sedif.sistema_tickets.core.documento;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.actividad.ActividadExtra;

@Service
public class DocumentoService {

    // Fuentes globales
    private final Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private final Font fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private final Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

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
    // 1. GENERACIÓN DE DICTAMEN TÉCNICO (CON EL NUEVO ENCABEZADO OFICIAL)
    // =========================================================================================
    public byte[] generarDictamenTecnicoPdf(DictamenRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7, Color.BLACK);
            Font fontNormalDoc = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            // --- 1. LOGO GRANDE Y CENTRADO ---
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
                System.err.println("Error cargando logo: " + e.getMessage());
            }
            tableLogo.addCell(cellLogo);
            document.add(tableLogo);

            // --- 2. TEXTOS A LA DERECHA (BLOQUE COMPACTO) ---
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
            agregarLineaDatoUsuario(tUsuario, "DIRECCIÓN:", request.direccionUsuario(), fontNormalDoc);
            agregarLineaDatoUsuario(tUsuario, "DEPARTAMENTO:", request.departamentoUsuario(), fontNormalDoc);
            agregarLineaDatoUsuario(tUsuario, "NOMBRE DE USUARIO:", request.nombreUsuario(), fontNormalDoc);
            agregarLineaDatoUsuario(tUsuario, "TELEFONO / EXT:", request.telefonoUsuario(), fontNormalDoc);
            agregarLineaDatoUsuario(tUsuario, "TIPO DE REPORTE:", request.tipoReporte(), fontNormalDoc);
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
                PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", fontNormalDoc));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            document.add(tEquipo);

            PdfPTable tTextos = new PdfPTable(1);
            tTextos.setWidthPercentage(100);
            Paragraph cuerpoTextos = new Paragraph();
            cuerpoTextos.add(new Chunk("DESCRIPCIÓN DE LA FALLA:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.fallaReportada() != null ? request.fallaReportada() : "") + "\n\n", fontNormalDoc));
            cuerpoTextos.add(new Chunk("DIAGNÓSTICO TÉCNICO:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.diagnostico() != null ? request.diagnostico() : "") + "\n\n", fontNormalDoc));
            cuerpoTextos.add(new Chunk("HALLAZGOS:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.hallazgos() != null ? request.hallazgos() : "") + "\n\n", fontNormalDoc));
            cuerpoTextos.add(new Chunk("CONCLUSIÓN:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.conclusion() != null ? request.conclusion() : ""), fontNormalDoc));
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

    // =========================================================================================
    // 2. GENERACIÓN DE MANTENIMIENTO PREVENTIVO
    // =========================================================================================
    public byte[] generarMantenimientoPdf(MantenimientoPreventivoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36); 
            PdfWriter.getInstance(document, baos);
            document.open();
            
            Color colorGuinda = new Color(128, 26, 54);

            Font fontTituloDoc = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
            Font fontNormalDoc = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            Paragraph pDir = new Paragraph("DIRECCIÓN DE ADMINISTRACIÓN Y FINANZAS\nDEPARTAMENTO DE SOPORTE TÉCNICO\nLISTADO DE EQUIPOS DE CÓMPUTO AL QUE SE LE DIO MANTENIMIENTO PREVENTIVO\n(" + (request.departamento() != null ? request.departamento().toUpperCase() : "") + ")", fontTituloDoc);
            pDir.setAlignment(Element.ALIGN_CENTER);
            document.add(pDir);
            
            Paragraph pFechas = new Paragraph((request.fechaInicio() != null ? request.fechaInicio() : "") + " - " + (request.fechaFin() != null ? request.fechaFin() : ""), fontNormalDoc);
            pFechas.setAlignment(Element.ALIGN_RIGHT);
            pFechas.setSpacingAfter(10f);
            document.add(pFechas);

            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 3.5f, 4.5f, 2.5f, 2.5f, 3f, 3f, 2f, 2.5f, 3f});

            String[] cabeceras = {"No.", "ÁREA", "NOMBRE DE USUARIO RESPONSABLE", "TIPO DE CPU", "MARCA", "MODELO", "NO. DE SERIE", "MEMORIA RAM", "CAPACIDAD DISCO DURO", "NO. DE INVENTARIO DE EQUIPO"};
            for (String cab : cabeceras) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontCabecera));
                cell.setBackgroundColor(colorGuinda);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            int index = 1;
            if (request.equipos() != null) {
                for (EquipoMantenimientoDTO equipo : request.equipos()) {
                    table.addCell(crearCeldaTabla(String.valueOf(index++), fontNormalDoc, Element.ALIGN_CENTER));
                    table.addCell(crearCeldaTabla(equipo.area(), fontNormalDoc, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.usuarioResponsable(), fontNormalDoc, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.tipoCpu(), fontNormalDoc, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.marca(), fontNormalDoc, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.modelo(), fontNormalDoc, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.numeroSerie(), fontNormalDoc, Element.ALIGN_LEFT));
                    table.addCell(crearCeldaTabla(equipo.memoriaRam(), fontNormalDoc, Element.ALIGN_CENTER));
                    table.addCell(crearCeldaTabla(equipo.capacidadDisco(), fontNormalDoc, Element.ALIGN_CENTER));
                    table.addCell(crearCeldaTabla(equipo.numeroInventario(), fontNormalDoc, Element.ALIGN_CENTER));
                }
            }
            document.add(table);
            document.add(new Paragraph("\n\n\n\n")); 

            PdfPTable tFirmas = new PdfPTable(2);
            tFirmas.setWidthPercentage(100);
            Font fontFirma = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            
            String textoRealizo = "REALIZÓ EL SERVICIO\nSOPORTE TÉCNICO\n\n\n\n___________________________________\n" 
                                + (request.nombreTecnico() != null ? request.nombreTecnico().toUpperCase() : "") 
                                + "\nSOPORTE TÉCNICO";
            PdfPCell cRealizo = new PdfPCell(new Phrase(textoRealizo, fontFirma));
            cRealizo.setBorder(Rectangle.NO_BORDER);
            cRealizo.setHorizontalAlignment(Element.ALIGN_CENTER);

            String textoConformidad = "FIRMA DE CONFORMIDAD\nPOR PARTE DEL USUARIO\n\n\n\n___________________________________\n" 
                                    + (request.departamento() != null ? request.departamento().toUpperCase() : "");
            PdfPCell cConformidad = new PdfPCell(new Phrase(textoConformidad, fontFirma));
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

    // =========================================================
    // 3. MÉTODO PARA ENTRADA DE EQUIPO DE CÓMPUTO
    // =========================================================
    public byte[] generarEntradaEquipoPdf(EntradaEquipoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36); 
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 8, Color.BLACK);
            Font fontNormalDoc = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

            // --- 1. ENCABEZADO INSTITUCIONAL ---
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
                System.err.println("Error cargando logo: " + e.getMessage());
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
            textos.setLeading(10f); 
            
            Font fontHeaderBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            textos.add(new Chunk("Unidad de Planeación, Administración y Finanzas\n", fontHeaderBold));
            textos.add(new Chunk("Dirección de Recursos Materiales, Servicios Generales, Archivo y Soporte Técnico\n", fontHeaderBold));
            textos.add(new Chunk("Departamento de Soporte Técnico\n", fontHeaderBold));
            textos.add(new Chunk("Heroica Puebla de Zaragoza, a " + (request.getFecha() != null ? request.getFecha() : ""), fontHeaderBold));
            
            cellTexto.addElement(textos);
            tableTextos.addCell(cellTexto);
            document.add(tableTextos);

            document.add(new Paragraph("\n"));
            
            Paragraph titulo = new Paragraph("ENTRADA DE EQUIPO DE CÓMPUTO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(10);
            document.add(titulo);

            // --- 2. FECHA Y FOLIO ---
            PdfPTable tFechaFolio = new PdfPTable(2);
            tFechaFolio.setWidthPercentage(100);
            
            PdfPCell cFecha = new PdfPCell(new Phrase("FECHA: " + (request.getFecha() != null ? request.getFecha() : "_________________"), fontBold));
            cFecha.setBorder(Rectangle.BOTTOM | Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
            
            String folioStr = String.format("%04d", request.getFolioTicket() != null ? request.getFolioTicket() : 0);
            PdfPCell cFolio = new PdfPCell(new Phrase("No. FOLIO: " + folioStr, fontBold));
            cFolio.setBorder(Rectangle.BOTTOM | Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
            cFolio.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            tFechaFolio.addCell(cFecha);
            tFechaFolio.addCell(cFolio);
            document.add(tFechaFolio);
            document.add(new Paragraph("\n"));

            // --- 3. NUEVO DISEÑO: UBICACIONES EN DOS COLUMNAS ---
            // Creamos una tabla de 4 columnas (Etiqueta1, Valor1, Etiqueta2, Valor2)
            PdfPTable tUbicacion = new PdfPTable(4);
            tUbicacion.setWidthPercentage(100);
            tUbicacion.setWidths(new float[]{1.5f, 3.5f, 1.5f, 3.5f});
            
            // Títulos de las dos secciones
            PdfPCell titActual = new PdfPCell(new Phrase("UBICACION ACTUAL", fontBoldItalic));
            titActual.setColspan(2);
            titActual.setBorder(Rectangle.NO_BORDER);
            tUbicacion.addCell(titActual);
            
            PdfPCell titDestino = new PdfPCell(new Phrase("UBICACION DESTINO", fontBoldItalic));
            titDestino.setColspan(2);
            titDestino.setBorder(Rectangle.NO_BORDER);
            tUbicacion.addCell(titDestino);
            
            // Llenado de filas (Izquierda: Actual, Derecha: Destino)
            agregarLineaDatoUsuario(tUbicacion, "DEPARTAMENTO:", request.getDepartamentoActual(), fontNormalDoc);
            agregarLineaDatoUsuario(tUbicacion, "A RESGUARDO DE:", request.getResguardoDestino(), fontNormalDoc);
            
            agregarLineaDatoUsuario(tUbicacion, "DIRECCIÓN:", request.getDireccionActual(), fontNormalDoc);
            agregarLineaDatoUsuario(tUbicacion, "DIRECCIÓN:", request.getDireccionDestino(), fontNormalDoc);
            
            agregarLineaDatoUsuario(tUbicacion, "TELEFONO:", request.getTelefonoActual(), fontNormalDoc);
            agregarLineaDatoUsuario(tUbicacion, "TELEFONO:", request.getTelefonoDestino(), fontNormalDoc);
            
            agregarLineaDatoUsuario(tUbicacion, "RESPONSABLE:", request.getResponsableActual(), fontNormalDoc);
            agregarLineaDatoUsuario(tUbicacion, "RESPONSABLE:", request.getResponsableDestino(), fontNormalDoc);
            
            document.add(tUbicacion);
            
            // Espacio antes de la tabla de equipos
            Paragraph espacio = new Paragraph(" ");
            espacio.setSpacingAfter(5f);
            document.add(espacio);

            // --- 4. TABLA CENTRAL DE EQUIPOS ---
            PdfPTable tEquipo = new PdfPTable(new float[]{1f, 3.5f, 2f, 2f, 2.5f, 2.5f, 1f});
            tEquipo.setWidthPercentage(100);
            
            String[] cabecerasEquipo = {"CVE.", "DESCRIPCION", "MARCA", "MODELO", "No. SERIE", "NO. DE RESGUARDO", "CANT."};
            for (String cab : cabecerasEquipo) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontBold));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            
            int filasLlenadas = 0;
            if (request.getEquipos() != null) {
                for (EntradaEquipoDetalleDTO equipo : request.getEquipos()) {
                    tEquipo.addCell(new Phrase(equipo.getCve() != null ? equipo.getCve() : "", fontNormalDoc));
                    tEquipo.addCell(new Phrase(equipo.getDescripcion() != null ? equipo.getDescripcion() : "", fontNormalDoc));
                    tEquipo.addCell(new Phrase(equipo.getMarca() != null ? equipo.getMarca() : "", fontNormalDoc));
                    tEquipo.addCell(new Phrase(equipo.getModelo() != null ? equipo.getModelo() : "", fontNormalDoc));
                    tEquipo.addCell(new Phrase(equipo.getSerie() != null ? equipo.getSerie() : "", fontNormalDoc));
                    tEquipo.addCell(new Phrase(equipo.getNoResguardo() != null ? equipo.getNoResguardo() : "", fontNormalDoc));
                    tEquipo.addCell(new Phrase(equipo.getCantidad() != null ? equipo.getCantidad() : "", fontNormalDoc));
                    filasLlenadas++;
                }
            }
            
            // Rellenar filas vacías hasta llegar a 10
            for (int i = filasLlenadas; i < 10; i++) {
                for(int j = 0; j < 7; j++) {
                    PdfPCell emptyCell = new PdfPCell(new Phrase(" "));
                    emptyCell.setMinimumHeight(15f);
                    tEquipo.addCell(emptyCell);
                }
            }
            document.add(tEquipo);
            document.add(new Paragraph("\n\n\n\n")); // Espacio amplio para las firmas

            // --- 5. FIRMAS ---
            PdfPTable tFirmas = new PdfPTable(2);
            tFirmas.setWidthPercentage(100);
            
            String fEntregado = formatearNombreFirma(request.getEntregadoPor());
            String fRecibido = formatearNombreFirma(request.getRecibidoPor());
            
            PdfPCell cFirmaE = new PdfPCell(new Phrase("ENTREGADO POR:\n(Nombre y Firma)\n\n\n___________________________\n" + fEntregado, fontBoldItalic));
            cFirmaE.setBorder(Rectangle.NO_BORDER);
            cFirmaE.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            PdfPCell cFirmaR = new PdfPCell(new Phrase("RECIBIDO POR:\n(Nombre y Firma)\n\n\n___________________________\n" + fRecibido, fontBoldItalic));
            cFirmaR.setBorder(Rectangle.NO_BORDER);
            cFirmaR.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            tFirmas.addCell(cFirmaE);
            tFirmas.addCell(cFirmaR);
            document.add(tFirmas);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el formato oficial de Entrada de Equipo", e);
        }
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
            
            Color colorGuinda = new Color(128, 26, 54);

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.BLACK);
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK); 

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
                cell.setPadding(4);
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

    // =========================================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================================
    private void agregarLineaDatoUsuario(PdfPTable table, String label, String value, Font font) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7)));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        table.addCell(cellLabel);
        
        PdfPCell cellValue = new PdfPCell(new Phrase(value != null ? value : "", font));
        cellValue.setBorder(Rectangle.NO_BORDER);
        table.addCell(cellValue);
    }

    private String formatearNombreFirma(String nombre) {
        // CORRECCIÓN: Si no hay nombre, devolvemos texto vacío en lugar de otra línea.
        return (nombre != null && !nombre.trim().isEmpty()) ? nombre.toUpperCase() : "";
    }

    private PdfPCell crearCelda(String texto, Font fuente, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell crearCeldaTabla(String texto, Font font, int alineacion) {
        String textoLimpio = texto != null ? texto.toUpperCase().trim() : "";
        PdfPCell cell = new PdfPCell(new Phrase(textoLimpio, font));
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        return cell;
    }
}