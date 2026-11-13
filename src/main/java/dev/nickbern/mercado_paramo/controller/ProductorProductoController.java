package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.ProductoRequest;
import dev.nickbern.mercado_paramo.dto.ProductoResponse;
import dev.nickbern.mercado_paramo.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productor/productos")
@RequiredArgsConstructor
public class ProductorProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> misProductos(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(productoService.misProductos(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crear(principal.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        productoService.desactivar(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<ProductoResponse> reactivar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(productoService.reactivar(principal.getUsername(), id));
    }
}
