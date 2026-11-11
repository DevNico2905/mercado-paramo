package dev.nickbern.mercado_paramo.controller;

import dev.nickbern.mercado_paramo.dto.CategoriaResponse;
import dev.nickbern.mercado_paramo.dto.ProductoResponse;
import dev.nickbern.mercado_paramo.service.CategoriaService;
import dev.nickbern.mercado_paramo.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> categorias() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    @GetMapping("/productos")
    public ResponseEntity<List<ProductoResponse>> catalogo(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long productorId) {
        return ResponseEntity.ok(productoService.catalogo(categoriaId, productorId));
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.detalle(id));
    }
}
