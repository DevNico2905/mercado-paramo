package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.ReporteVentasResponse;
import dev.nickbern.mercado_paramo.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/productor/reportes")
@RequiredArgsConstructor
public class ProductorReporteController {

    private final ReporteService reporteService;

    @GetMapping("/ventas")
    public ResponseEntity<ReporteVentasResponse> ventas(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.ventasDelProductor(principal.getUsername(), desde, hasta));
    }
}
