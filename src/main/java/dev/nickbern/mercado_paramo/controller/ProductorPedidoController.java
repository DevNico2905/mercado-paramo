package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.CambioEstadoRequest;
import dev.nickbern.mercado_paramo.dto.FiltroPedidos;
import dev.nickbern.mercado_paramo.dto.PedidoResponse;
import dev.nickbern.mercado_paramo.model.EstadoPedido;
import dev.nickbern.mercado_paramo.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productor/pedidos")
@RequiredArgsConstructor
public class ProductorPedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> recibidos(
            @AuthenticationPrincipal UserDetails principal,
            @ParameterObject FiltroPedidos filtro) {
        return ResponseEntity.ok(pedidoService.pedidosRecibidos(principal.getUsername(), filtro));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(
                pedidoService.cambiarEstadoComoProductor(principal.getUsername(), id, request.estado()));
    }
}
