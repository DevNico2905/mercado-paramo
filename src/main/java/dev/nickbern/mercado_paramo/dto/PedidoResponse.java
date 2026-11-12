package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.EstadoPedido;
import dev.nickbern.mercado_paramo.model.Pedido;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        LocalDateTime fecha,
        EstadoPedido estado,
        BigDecimal total,
        BigDecimal comision,
        BigDecimal montoProductor,
        String direccionEntrega,
        String compradorNombre,
        String compradorTelefono,
        Long productorId,
        String productorNombre,
        String vereda,
        List<DetallePedidoResponse> detalles
) {
    public static PedidoResponse de(Pedido pedido) {
        Usuario comprador = pedido.getComprador();
        Productor productor = pedido.getProductor();
        return new PedidoResponse(
                pedido.getId(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                pedido.getComision(),
                pedido.getTotal().subtract(pedido.getComision()),
                pedido.getDireccionEntrega(),
                comprador.getNombre(),
                comprador.getTelefono(),
                productor.getId(),
                productor.getUsuario().getNombre(),
                productor.getVereda(),
                pedido.getDetalles().stream().map(DetallePedidoResponse::de).toList());
    }
}
