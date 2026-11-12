package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.DetallePedido;
import dev.nickbern.mercado_paramo.model.Producto;

import java.math.BigDecimal;

public record DetallePedidoResponse(
        Long productoId,
        String productoNombre,
        String unidad,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetallePedidoResponse de(DetallePedido detalle) {
        Producto producto = detalle.getProducto();
        return new DetallePedidoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getUnidad(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }
}
