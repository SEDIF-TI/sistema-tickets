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
import com.sedif.sistema_tickets.core.resguardo.ResguardoRequest;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Composicion de la responsiva de resguardo en PDF.
 *
 * <p>Deja constancia de que un equipo queda bajo la custodia de una persona:
 * identificacion del usuario, caracteristicas y vigencia del equipo, y la
 * declaratoria de responsabilidad que se firma.</p>
 *
 * <p>El apartado de firmas se resuelve solo. Quien entrega es el usuario de la
 * sesion, cuyo nombre completo se recupera de la base a partir del correo del
 * token; quien recibe es el solicitante indicado en la peticion.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResguardoPdf {

    /** Denominacion con la que se firma cuando no se resuelve el perfil. */
    private static final String OPERADOR_GENERICO = "SOPORTE TÉCNICO";

    private static final String DECLARATORIA =
            "Por medio de la presente, me comprometo a resguardar y hacer buen uso del equipo "
            + "descrito anteriormente, el cual me es asignado para el desempeño de mis funciones. "
            + "En caso de robo, extravío o daño derivado del mal uso, me haré responsable de la "
            + "reparación o reposición del mismo conforme a la normativa aplicable.\n";

    private final UsuarioRepository usuarioRepository;

    public byte[] generar(ResguardoRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
            Font fontBoldItalic = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7, Color.BLACK);
            Font fontNorm = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            ElementosPdf.agregarLogotipo(document, 220, 110);

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

            Paragraph titulo = new Paragraph("RESGUARDO DE EQUIPO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(15);
            document.add(titulo);

            PdfPTable tFechaFolio = new PdfPTable(2);
            tFechaFolio.setWidthPercentage(100);
            PdfPCell cFecha = new PdfPCell(new Phrase("FECHA: " + fechaActual, fontBold));
            cFecha.setBorder(Rectangle.BOX);

            PdfPCell cEmpleado = new PdfPCell(new Phrase("No. EMPLEADO: "
                    + (request.solicitanteNumero() != null ? request.solicitanteNumero() : "N/A"), fontBold));
            cEmpleado.setBorder(Rectangle.BOX);
            cEmpleado.setHorizontalAlignment(Element.ALIGN_RIGHT);

            tFechaFolio.addCell(cFecha);
            tFechaFolio.addCell(cEmpleado);
            document.add(tFechaFolio);
            document.add(new Paragraph("\n"));

            document.add(new Phrase("DATOS DEL USUARIO\n", fontBold));
            PdfPTable tUsuario = new PdfPTable(new float[]{3f, 7f});
            tUsuario.setWidthPercentage(100);

            // Aqui la etiqueta va en negrita y el valor ausente se marca como
            // N/A, a diferencia del par etiqueta/valor generico.
            java.util.function.BiConsumer<String, String> agregarFilaUsu = (lbl, val) -> {
                PdfPCell cLbl = new PdfPCell(new Phrase(lbl, fontBold));
                cLbl.setBorder(Rectangle.NO_BORDER);
                PdfPCell cVal = new PdfPCell(new Phrase(val != null ? val : "N/A", fontNorm));
                cVal.setBorder(Rectangle.NO_BORDER);
                tUsuario.addCell(cLbl);
                tUsuario.addCell(cVal);
            };

            agregarFilaUsu.accept("NOMBRE DE USUARIO:", ElementosPdf.nombreConTratamiento(request.solicitanteNombre()));
            agregarFilaUsu.accept("DEPARTAMENTO:", request.departamento());
            agregarFilaUsu.accept("CELULAR:", request.telefono());
            document.add(tUsuario);
            document.add(new Paragraph("\n"));

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
                // La comparacion con "null" cubre el String.valueOf de una
                // cantidad ausente, que produce ese texto en lugar de un nulo.
                PdfPCell cell = new PdfPCell(new Phrase(
                        val != null && !val.equals("null") ? val : "N/A", fontNorm));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tEquipo.addCell(cell);
            }
            document.add(tEquipo);

            PdfPTable tTextos = new PdfPTable(1);
            tTextos.setWidthPercentage(100);
            Paragraph cuerpoTextos = new Paragraph();
            cuerpoTextos.add(new Chunk("ACCESORIOS Y OBSERVACIONES:\n", fontBold));
            cuerpoTextos.add(new Chunk((request.accesorios() != null ? request.accesorios() : "SIN ACCESORIOS ADICIONALES") + "\n\n", fontNorm));

            cuerpoTextos.add(new Chunk("DECLARATORIA DE RESGUARDO:\n", fontBold));
            cuerpoTextos.add(new Chunk(DECLARATORIA, fontNorm));

            PdfPCell cellTextos = new PdfPCell(cuerpoTextos);
            cellTextos.setPadding(10);
            cellTextos.setMinimumHeight(180f);
            tTextos.addCell(cellTextos);
            document.add(tTextos);
            document.add(new Paragraph("\n"));

            PdfPTable tFirmas = new PdfPTable(2);
            tFirmas.setWidthPercentage(100);
            tFirmas.setSpacingBefore(15f);

            // Columna izquierda: quien entrega, tomado del perfil de la sesion.
            PdfPCell cEntrega = new PdfPCell();
            cEntrega.setBorder(Rectangle.NO_BORDER);
            cEntrega.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pEntrega = new Paragraph();
            pEntrega.setAlignment(Element.ALIGN_CENTER);
            pEntrega.setLeading(13f);
            pEntrega.add(new Chunk("ENTREGA:\n(Nombre y Firma)\n\n\n\n\n", fontBoldItalic));
            pEntrega.add(new Chunk("___________________________\n", fontBoldItalic));
            pEntrega.add(new Chunk(ElementosPdf.nombreConTratamiento(nombreDeQuienEntrega()), fontBoldItalic));

            cEntrega.addElement(pEntrega);
            tFirmas.addCell(cEntrega);

            // Columna derecha: quien recibe el equipo bajo su resguardo.
            PdfPCell cRecibe = new PdfPCell();
            cRecibe.setBorder(Rectangle.NO_BORDER);
            cRecibe.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pRecibe = new Paragraph();
            pRecibe.setAlignment(Element.ALIGN_CENTER);
            pRecibe.setLeading(13f);
            pRecibe.add(new Chunk("RECIBE (USUARIO):\n(Nombre y Firma)\n\n\n\n\n", fontBoldItalic));
            pRecibe.add(new Chunk("___________________________\n", fontBoldItalic));
            pRecibe.add(new Chunk(ElementosPdf.nombreConTratamiento(request.solicitanteNombre()), fontBoldItalic));

            cRecibe.addElement(pRecibe);
            tFirmas.addCell(cRecibe);

            document.add(tFirmas);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la responsiva de resguardo.", e);
        }
    }

    /**
     * Nombre completo de quien firma la entrega, resuelto desde la sesion.
     *
     * <p>Si no hay sesion o el perfil no se encuentra, se firma con la
     * denominacion generica del area en lugar de impedir la emision del
     * documento.</p>
     */
    private String nombreDeQuienEntrega() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
                return OPERADOR_GENERICO;
            }

            return usuarioRepository.findByCorreo(auth.getName())
                    .map(this::nombreCompleto)
                    .orElse(OPERADOR_GENERICO);

        } catch (Exception e) {
            log.warn("No se pudo determinar el perfil del usuario para el documento.", e);
            return OPERADOR_GENERICO;
        }
    }

    /**
     * Une nombre y apellidos. Los apellidos se anaden solo si constan, para no
     * dejar espacios sueltos en la linea de firma.
     */
    private String nombreCompleto(Usuario usuario) {
        StringBuilder completo = new StringBuilder(usuario.getNombre());

        if (usuario.getApellidoPaterno() != null && !usuario.getApellidoPaterno().trim().isEmpty()) {
            completo.append(" ").append(usuario.getApellidoPaterno());
        }
        if (usuario.getApellidoMaterno() != null && !usuario.getApellidoMaterno().trim().isEmpty()) {
            completo.append(" ").append(usuario.getApellidoMaterno());
        }

        return completo.toString();
    }
}
