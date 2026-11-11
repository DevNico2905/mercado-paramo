package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.Producto;
import dev.nickbern.mercado_paramo.model.Productor;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String nombre,
        String unidad,
        BigDecimal precio,
        int stock,
        boolean activo,
        Long categoriaId,
        String categoriaNombre,
        Long productorId,
        String productorNombre,
        String vereda
) {
    public static ProductoResponse de(Producto producto) {
        Productor productor = producto.getProductor();
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getUnidad(),
                producto.getPrecio(),
                producto.getStock(),
                producto.isActivo(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                productor.getId(),
                productor.getUsuario().getNombre(),
                productor.getVereda());
    }
}