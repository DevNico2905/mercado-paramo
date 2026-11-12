package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.FiltroPedidos;
import dev.nickbern.mercado_paramo.dto.PedidoResponse;
import dev.nickbern.mercado_paramo.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
public class AdminPedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> todos(@ParameterObject FiltroPedidos filtro) {
        return ResponseEntity.ok(pedidoService.todos(filtro));
    }
}