package com.sedif.sistema_tickets.core.documento;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class DocumentoService {
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
            
            // 1. Definimos el Color Institucional
            Color colorGuinda = new Color(128, 26, 54);

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            // AQUÍ ESTÁ LA CORRECCIÓN 1: La letra de la cabecera ahora es BLANCA
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            // 1. TÍTULOS PRINCIPALES
            Paragraph pDir = new Paragraph("DIRECCIÓN DE ADMINISTRACIÓN Y FINANZAS\nDEPARTAMENTO DE SOPORTE TÉCNICO\nLISTADO DE EQUIPOS DE CÓMPUTO AL QUE SE LE DIO MANTENIMIENTO PREVENTIVO\n(" + (request.departamento() != null ? request.departamento().toUpperCase() : "") + ")", fontTitulo);
            pDir.setAlignment(Element.ALIGN_CENTER);
            document.add(pDir);
            
            // Rango de Fechas
            Paragraph pFechas = new Paragraph((request.fechaInicio() != null ? request.fechaInicio() : "") + " - " + (request.fechaFin() != null ? request.fechaFin() : ""), fontNormal);
            pFechas.setAlignment(Element.ALIGN_RIGHT);
            pFechas.setSpacingAfter(10f);
            document.add(pFechas);

            // 2. TABLA PRINCIPAL (10 Columnas)
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 3.5f, 4.5f, 2.5f, 2.5f, 3f, 3f, 2f, 2.5f, 3f});

            // Encabezados de la tabla
            String[] cabeceras = {"No.", "ÁREA", "NOMBRE DE USUARIO RESPONSABLE", "TIPO DE CPU", "MARCA", "MODELO", "NO. DE SERIE", "MEMORIA RAM", "CAPACIDAD DISCO DURO", "NO. DE INVENTARIO DE EQUIPO"};
            for (String cab : cabeceras) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, fontCabecera));
                // AQUÍ ESTÁ LA CORRECCIÓN 2: Aplicamos el color Guinda al fondo
                cell.setBackgroundColor(colorGuinda);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            // Filas
            int index = 1;
            if (request.equipos() != null) {
                for (EquipoMantenimientoDTO equipo : request.equipos()) {
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

            // 3. FIRMAS 
            PdfPTable tFirmas = new PdfPTable(2);
            tFirmas.setWidthPercentage(100);

            // Firma Izquierda (Para que se vea en negro, volvemos a usar la lógica con FontFactory aquí)
            Font fontFirma = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);

            String textoRealizo = "REALIZÓ EL SERVICIO\nSOPORTE TÉCNICO\n\n\n\n___________________________________\n" 
                                + (request.nombreTecnico() != null ? request.nombreTecnico().toUpperCase() : "") 
                                + "\nSOPORTE TÉCNICO";
            PdfPCell cRealizo = new PdfPCell(new Phrase(textoRealizo, fontFirma));
            cRealizo.setBorder(Rectangle.NO_BORDER);
            cRealizo.setHorizontalAlignment(Element.ALIGN_CENTER);

            // Firma Derecha 
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

    // =========================================================
    // MÉTODO PARA ENTRADA DE EQUIPO DE CÓMPUTO
    // =========================================================
    public byte[] generarEntradaEquipoPdf(EntradaEquipoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36); 
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 8, Color.BLACK);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

            // 1. ENCABEZADO REUTILIZADO
            agregarEncabezadoInstitucional(document, "ENTRADA DE EQUIPO DE COMPUTO");

            // 2. FECHA Y FOLIO
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

            // 3. LEYENDA DE ACRÓNIMOS 
            PdfPTable tLeyenda = new PdfPTable(3);
            tLeyenda.setWidthPercentage(100);
            String[] leyendas = {
                "SP SERVICIO PREVENTIVO", "CP CAMBIO PERMANENTE", "OT OTROS (ESPECIFIQUE)",
                "SC SERVICIO CORRECTIVO", "PU PRESTAMO A USUARIOS", "",
                "RG RECLAMO DE GARANTIA", "AE ALTA DE EQUIPO", "",
                "CT CAMBIO TEMPORAL", "PP PRESTAMO DEL PROVEEDOR", ""
            };
            for (String texto : leyendas) {
                PdfPCell cell = new PdfPCell(new Phrase(texto, fontBoldItalic));
                cell.setBorder(Rectangle.NO_BORDER);
                tLeyenda.addCell(cell);
            }
            document.add(tLeyenda);
            document.add(new Paragraph("\n"));

            // 4. TABLA CENTRAL DE EQUIPOS
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
                    tEquipo.addCell(new Phrase(equipo.getCve() != null ? equipo.getCve() : "", fontNormal));
                    tEquipo.addCell(new Phrase(equipo.getDescripcion() != null ? equipo.getDescripcion() : "", fontNormal));
                    tEquipo.addCell(new Phrase(equipo.getMarca() != null ? equipo.getMarca() : "", fontNormal));
                    tEquipo.addCell(new Phrase(equipo.getModelo() != null ? equipo.getModelo() : "", fontNormal));
                    tEquipo.addCell(new Phrase(equipo.getSerie() != null ? equipo.getSerie() : "", fontNormal));
                    tEquipo.addCell(new Phrase(equipo.getNoResguardo() != null ? equipo.getNoResguardo() : "", fontNormal));
                    tEquipo.addCell(new Phrase(equipo.getCantidad() != null ? equipo.getCantidad() : "", fontNormal));
                    filasLlenadas++;
                }
            }
            
            for (int i = filasLlenadas; i < 10; i++) {
                for(int j = 0; j < 7; j++) {
                    PdfPCell emptyCell = new PdfPCell(new Phrase(" "));
                    emptyCell.setMinimumHeight(15f);
                    tEquipo.addCell(emptyCell);
                }
            }
            document.add(tEquipo);
            document.add(new Paragraph("\n"));

            // 5. SECCIÓN DE UBICACIONES 
            PdfPTable tUbicacion = new PdfPTable(new float[]{3f, 7f});
            tUbicacion.setWidthPercentage(100);
            
            PdfPCell titActual = new PdfPCell(new Phrase("UBICACION ACTUAL", fontBoldItalic));
            titActual.setColspan(2);
            titActual.setBorder(Rectangle.NO_BORDER);
            tUbicacion.addCell(titActual);
            
            agregarLineaDatoUsuario(tUbicacion, "DEPARTAMENTO:", request.getDepartamentoActual(), fontNormal);
            agregarLineaDatoUsuario(tUbicacion, "DIRECCIÓN:", request.getDireccionActual(), fontNormal);
            agregarLineaDatoUsuario(tUbicacion, "TELEFONO:", request.getTelefonoActual(), fontNormal);
            agregarLineaDatoUsuario(tUbicacion, "RESPONSABLE:", request.getResponsableActual(), fontNormal);
            
            PdfPCell titDestino = new PdfPCell(new Phrase("UBICACION DESTINO", fontBoldItalic));
            titDestino.setColspan(2);
            titDestino.setBorder(Rectangle.NO_BORDER);
            tUbicacion.addCell(titDestino);
            
            agregarLineaDatoUsuario(tUbicacion, "A RESGUARDO DE:", request.getResguardoDestino(), fontNormal);
            agregarLineaDatoUsuario(tUbicacion, "DIRECCIÓN:", request.getDireccionDestino(), fontNormal);
            agregarLineaDatoUsuario(tUbicacion, "TELEFONO:", request.getTelefonoDestino(), fontNormal);
            agregarLineaDatoUsuario(tUbicacion, "RESPONSABLE:", request.getResponsableDestino(), fontNormal);
            
            document.add(tUbicacion);
            document.add(new Paragraph("\n"));

            // 6. NOTAS 
            document.add(new Phrase("NOTA:\n", fontBoldItalic));
            
            PdfPTable tNotas = new PdfPTable(2);
            tNotas.setWidthPercentage(100);
            tNotas.setSpacingBefore(5f);
            
            // --- CAJA DE CONCEPTO ---
            PdfPCell cConcepto = new PdfPCell();
            cConcepto.setBorder(Rectangle.NO_BORDER);
            cConcepto.addElement(new Phrase("CONCEPTO:", fontBoldItalic));
            
            // Creamos una mini-tabla interna para poder anidar la caja
            PdfPTable tablaInternaConcepto = new PdfPTable(1);
            tablaInternaConcepto.setWidthPercentage(95); // 95% para que tenga margen
            PdfPCell boxConcepto = new PdfPCell(new Phrase(request.getConcepto() != null ? request.getConcepto() : "", fontNormal));
            boxConcepto.setPadding(10);
            boxConcepto.setMinimumHeight(40f); // Le damos altura mínima para que parezca área de texto
            tablaInternaConcepto.addCell(boxConcepto);
            
            cConcepto.addElement(tablaInternaConcepto);
            
            // --- CAJA DE OBSERVACIÓN ---
            PdfPCell cObs = new PdfPCell();
            cObs.setBorder(Rectangle.NO_BORDER);
            cObs.addElement(new Phrase("OBSERVACION:", fontBoldItalic));
            
            // Creamos una mini-tabla interna para poder anidar la caja
            PdfPTable tablaInternaObs = new PdfPTable(1);
            tablaInternaObs.setWidthPercentage(95);
            PdfPCell boxObs = new PdfPCell(new Phrase(request.getObservacion() != null ? request.getObservacion() : "", fontNormal));
            boxObs.setPadding(10);
            boxObs.setMinimumHeight(40f);
            tablaInternaObs.addCell(boxObs);
            
            cObs.addElement(tablaInternaObs);
            
            // Agregamos ambas columnas a la tabla de notas
            tNotas.addCell(cConcepto);
            tNotas.addCell(cObs);
            document.add(tNotas);
            document.add(new Paragraph("\n\n"));

            // 7. FIRMAS
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
}