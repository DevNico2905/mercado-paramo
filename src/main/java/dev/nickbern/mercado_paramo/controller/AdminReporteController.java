package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.ImpactoResponse;
import dev.nickbern.mercado_paramo.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReporteController {

    private final ReporteService reporteService;

    @GetMapping("/impacto")
    public ResponseEntity<ImpactoResponse> impacto() {
        return ResponseEntity.ok(reporteService.impacto());
    }
}
