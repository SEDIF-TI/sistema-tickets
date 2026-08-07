package com.sedif.sistema_tickets.core.documento.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;

/**
 * Piezas que comparten los formatos oficiales: membrete, celdas de tabla,
 * bloques de firma y el formato de los nombres.
 *
 * <p>Cada documento se compone en su propia clase, pero todos reproducen la
 * misma identidad institucional. Reunir aqui esos elementos evita que el
 * membrete o el tratamiento de los nombres se separen entre formatos al tocar
 * uno solo.</p>
 *
 * <p>Solo contiene metodos estaticos: son funciones de maquetacion sin estado.</p>
 */
@Slf4j
public final class ElementosPdf {

    /** Ruta del logotipo dentro de los recursos del proyecto. */
    public static final String LOGOTIPO = "logo-puebla.png";

    private ElementosPdf() {
        // Clase de utilidades.
    }

    /** Membrete comun: nombre de la institucion y titulo del documento, centrados. */
    public static void agregarEncabezadoInstitucional(Document document, String titulo)
            throws DocumentException {

        Font fontInstitucion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Font fontTituloDoc = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);

        Paragraph pInstitucion = new Paragraph(
                "SISTEMA PARA EL DESARROLLO INTEGRAL DE LA FAMILIA\nDEL ESTADO DE PUEBLA",
                fontInstitucion);
        pInstitucion.setAlignment(Element.ALIGN_CENTER);

        Paragraph pTitulo = new Paragraph("\n" + titulo.toUpperCase(), fontTituloDoc);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingAfter(20f);

        document.add(pInstitucion);
        document.add(pTitulo);
    }

    /**
     * Anade el logotipo institucional centrado, dentro de una tabla de una
     * celda para poder controlar su separacion inferior.
     *
     * <p>Un fallo al cargar la imagen no interrumpe la composicion: el
     * documento sale sin logotipo, que es preferible a no emitirlo.</p>
     */
    public static void agregarLogotipo(Document document, float ancho, float alto)
            throws DocumentException {

        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);

        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPaddingBottom(15);

        try {
            com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(
                    ElementosPdf.class.getClassLoader().getResource(LOGOTIPO));
            logo.scaleToFit(ancho, alto);
            logo.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(logo);
        } catch (Exception e) {
            log.warn("No se pudo cargar el logotipo institucional en el documento.", e);
        }

        tabla.addCell(celda);
        document.add(tabla);
    }

    /** Celda de encabezado de tabla: negrita sobre fondo gris. */
    public static PdfPCell celdaCabecera(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    /** Celda de dato centrada, con borde. */
    public static PdfPCell celdaNormal(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    /** Celda sin borde, para los bloques que solo maquetan texto. */
    public static PdfPCell celdaSimple(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alineacion);
        return cell;
    }

    /** Celda auxiliar con fuente y alineacion propias, sin borde. */
    public static PdfPCell celdaAuxiliar(String texto, Font fuente, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alineacion);
        return cell;
    }

    /** Par etiqueta/valor sin bordes, para los bloques de datos del usuario. */
    public static void agregarLineaDato(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        PdfPCell celdaEtiqueta = new PdfPCell(
                new Phrase(etiqueta, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7)));
        celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "", fuente));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celdaValor);
    }

    /** Bloque de dos firmas al pie: quien entrega y quien recibe. */
    public static void agregarFirmas(Document doc, String nombre1, String nombre2)
            throws DocumentException {

        PdfPTable tFirmas = new PdfPTable(2);
        tFirmas.setWidthPercentage(80);
        tFirmas.setSpacingBefore(30);
        tFirmas.addCell(celdaSimple("ENTREGA:\n\n\n__________________\n" + nombre1, Element.ALIGN_CENTER));
        tFirmas.addCell(celdaSimple("RECIBE:\n\n\n__________________\n" + nombre2, Element.ALIGN_CENTER));
        doc.add(tFirmas);
    }

    /**
     * Nombre de firma en mayusculas. Sin dato, deja la linea para rellenarla a
     * mano sobre el papel.
     */
    public static String nombreDeFirma(String nombre) {
        return (nombre != null && !nombre.isEmpty())
                ? nombre.toUpperCase()
                : "___________________________";
    }

    /**
     * Antepone el tratamiento "C." al nombre, en mayusculas, sin duplicarlo si
     * ya viene puesto.
     */
    public static String nombreConTratamiento(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "N/A";
        }
        String limpio = nombre.trim().toUpperCase();
        return limpio.startsWith("C. ") ? limpio : "C. " + limpio;
    }
}
