package com.sedif.sistema_tickets.core.estatusticket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estatus")
@RequiredArgsConstructor
public class EstatusResource {

    private final EstatusService estatusService;

    @PostMapping
    public ResponseEntity<Estatus> crearEstatus(@RequestBody EstatusRecord record) {
        return ResponseEntity.ok(estatusService.crearEstatus(record));
    }
}