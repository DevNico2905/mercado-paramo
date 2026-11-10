package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.ProductorResponse;
import dev.nickbern.mercado_paramo.dto.VerificacionRequest;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.service.ProductorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/productores")
@RequiredArgsConstructor
public class AdminProductorController {

    private final ProductorService productorService;

    @GetMapping
    public ResponseEntity<List<ProductorResponse>> listar(
            @RequestParam(required = false) EstadoVerificacion estado) {
        return ResponseEntity.ok(productorService.listar(estado));
    }

    @PatchMapping("/{id}/verificacion")
    public ResponseEntity<ProductorResponse> verificar(
            @PathVariable Long id,
            @Valid @RequestBody VerificacionRequest request) {
        return ResponseEntity.ok(productorService.actualizarVerificacion(id, request.estado()));
    }
}
