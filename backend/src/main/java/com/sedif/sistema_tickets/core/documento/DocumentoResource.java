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

    // Usamos POST porque React nos va a enviar un JSON (Body) con los textos del memo
    @PostMapping(value = "/memorandum", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarMemorandum(@RequestBody MemorandumRequest request) {

        byte[] pdfGenerado = documentoService.generarMemorandum(request);

        // Configuramos las cabeceras para obligar al navegador a descargarlo
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Memorandum_Oficial.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfGenerado);
    }

    // --- Endpoint de la Requisición ---
    @PostMapping(value = "/requisicion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarRequisicion(@RequestBody RequisicionRequest request) {
        byte[] pdfGenerado = documentoService.generarRequisicion(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Requisicion_Material.pdf");
        return ResponseEntity.ok().headers(headers).body(pdfGenerado);
    }
}