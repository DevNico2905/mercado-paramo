package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.PedidoRequest;
import dev.nickbern.mercado_paramo.dto.PedidoResponse;
import dev.nickbern.mercado_paramo.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoService.crear(principal.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> misPedidos(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(pedidoService.misPedidos(principal.getUsername()));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.cancelar(principal.getUsername(), id));
    }
}