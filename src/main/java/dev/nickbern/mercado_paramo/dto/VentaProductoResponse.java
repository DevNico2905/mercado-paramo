package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.DetallePedido;
import dev.nickbern.mercado_paramo.model.Producto;

import java.math.BigDecimal;
import java.util.List;

public record VentaProductoResponse(
        Long productoId,
        String nombre,
        String unidad,
        int cantidadVendida,
        BigDecimal totalVendido
) {
    public static VentaProductoResponse de(List<DetallePedido> detalles) {
        Producto producto = detalles.get(0).getProducto();

        int cantidad = detalles.stream()
                .mapToInt(DetallePedido::getCantidad)
                .sum();

        BigDecimal total = detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new VentaProductoResponse(
                producto.getId(), producto.getNombre(), producto.getUnidad(), cantidad, total);
    }
}
