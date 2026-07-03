package com.sedif.sistema_tickets.core.documento;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoResource {

    private final DocumentoService documentoService;

    @PostMapping(value = "/memorandum", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarMemorandum(@RequestBody MemorandumRequest request) {
        byte[] pdfGenerado = documentoService.generarMemorandum(request);
        return construirRespuestaPdf(pdfGenerado, "Memorandum_Oficial.pdf");
    }

    @PostMapping(value = "/requisicion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarRequisicion(@RequestBody RequisicionRequest request) {
        byte[] pdfGenerado = documentoService.generarRequisicion(request);
        return construirRespuestaPdf(pdfGenerado, "Requisicion_Material.pdf");
    }

    @PostMapping(value = "/dictamen", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarDictamen(@RequestBody DictamenRequest request) {
        byte[] pdfGenerado = documentoService.generarDictamenTecnicoPdf(request);
        return construirRespuestaPdf(pdfGenerado, "Dictamen_Tecnico_" + request.folioTicket() + ".pdf");
    }

    @PostMapping(value = "/mantenimiento", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarMantenimiento(@RequestBody MantenimientoPreventivoRequest request) {
        // documentoService se encargará de iterar la lista y poner el número autoincrementable
        byte[] pdfGenerado = documentoService.generarMantenimientoPdf(request);
        return construirRespuestaPdf(pdfGenerado, "Mantenimiento_Preventivo.pdf");
    }

    /**
     * Método auxiliar para unificar la construcción de respuestas HTTP de documentos PDF.
     * Configura la disposición como 'inline' para permitir la visualización en el navegador.
     */
    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] documento, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=\"" + nombreArchivo + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(documento);
    }
}