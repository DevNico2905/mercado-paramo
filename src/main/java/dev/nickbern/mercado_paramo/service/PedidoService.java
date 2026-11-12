package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.ItemPedidoRequest;
import dev.nickbern.mercado_paramo.dto.PedidoRequest;
import dev.nickbern.mercado_paramo.dto.PedidoResponse;
import dev.nickbern.mercado_paramo.exception.*;
import dev.nickbern.mercado_paramo.model.*;
import dev.nickbern.mercado_paramo.repository.PedidoRepository;
import dev.nickbern.mercado_paramo.repository.ProductoRepository;
import dev.nickbern.mercado_paramo.repository.ProductorRepository;
import dev.nickbern.mercado_paramo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final BigDecimal TASA_COMISION = new BigDecimal("0.05");

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ProductorRepository productorRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public PedidoResponse crear(String emailComprador, PedidoRequest request) {
        Usuario comprador = usuarioRepository.findByEmail(emailComprador)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Map<Long, Integer> cantidades = cantidadesPorProducto(request.items());

        List<Producto> productos = productoRepository.findByIdIn(cantidades.keySet());
        if (productos.size() != cantidades.size()) {
            throw new RecursoNoEncontradoException("Uno o más productos no existen");
        }

        Pedido pedido = new Pedido();
        pedido.setComprador(comprador);
        pedido.setProductor(productorUnico(productos));
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setDireccionEntrega(request.direccionEntrega().strip());

        BigDecimal total = BigDecimal.ZERO;

        for (Producto producto : productos) {
            validarDisponible(producto);
            int cantidad = cantidades.get(producto.getId());

            if (producto.getStock() < cantidad) {
                throw new StockInsuficienteException(
                        "Stock insuficiente para: %s (disponible: %d)"
                                .formatted(producto.getNombre(), producto.getStock()));
            }

            producto.setStock(producto.getStock() - cantidad);

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            pedido.agregarDetalle(new DetallePedido(producto, cantidad, producto.getPrecio(), subtotal));
            total = total.add(subtotal);
        }

        pedido.setTotal(total);
        pedido.setComision(total.multiply(TASA_COMISION).setScale(2, RoundingMode.HALF_UP));

        return PedidoResponse.de(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> misPedidos(String emailComprador) {
        return pedidoRepository.findByCompradorEmailOrderByFechaDesc(emailComprador).stream()
                .map(PedidoResponse::de)
                .toList();
    }

    @Transactional
    public PedidoResponse cancelar(String emailComprador, Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);

        if (!pedido.getComprador().getEmail().equals(emailComprador)) {
            throw new AccesoDenegadoException("No puedes modificar pedidos de otro comprador");
        }

        cambiarEstado(pedido, EstadoPedido.CANCELADO);
        return PedidoResponse.de(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> pedidosRecibidos(String emailProductor, EstadoPedido estado) {
        Long productorId = productorDe(emailProductor).getId();

        List<Pedido> pedidos = (estado == null)
                ? pedidoRepository.findByProductorIdOrderByFechaDesc(productorId)
                : pedidoRepository.findByProductorIdAndEstadoOrderByFechaDesc(productorId, estado);

        return pedidos.stream().map(PedidoResponse::de).toList();
    }

    @Transactional
    public PedidoResponse cambiarEstadoComoProductor(String emailProductor, Long pedidoId, EstadoPedido nuevo) {
        if (nuevo == EstadoPedido.CANCELADO || nuevo == EstadoPedido.PENDIENTE) {
            throw new ReglaNegocioException("El productor solo puede ACEPTAR, RECHAZAR o marcar ENTREGADO un pedido");
        }

        Productor productor = productorDe(emailProductor);
        Pedido pedido = buscarPedido(pedidoId);

        if (!pedido.getProductor().getId().equals(productor.getId())) {
            throw new AccesoDenegadoException("No puedes modificar pedidos de otro productor");
        }

        cambiarEstado(pedido, nuevo);
        return PedidoResponse.de(pedido);
    }

    private void cambiarEstado(Pedido pedido, EstadoPedido nuevo) {
        EstadoPedido actual = pedido.getEstado();

        if (!actual.puedeCambiarA(nuevo)) {
            throw new TransicionInvalidaException(
                    "No se puede pasar un pedido de %s a %s".formatted(actual, nuevo));
        }

        pedido.setEstado(nuevo);

        if (nuevo.devuelveStock()) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto producto = detalle.getProducto();
                producto.setStock(producto.getStock() + detalle.getCantidad());
            }
        }
    }

    private Map<Long, Integer> cantidadesPorProducto(List<ItemPedidoRequest> items) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemPedidoRequest item : items) {
            if (cantidades.putIfAbsent(item.productoId(), item.cantidad()) != null) {
                throw new ReglaNegocioException("Producto repetido en el pedido: " + item.productoId());
            }
        }
        return cantidades;
    }

    private Productor productorUnico(List<Producto> productos) {
        Set<Long> productores = productos.stream()
                .map(producto -> producto.getProductor().getId())
                .collect(Collectors.toSet());

        if (productores.size() > 1) {
            throw new ReglaNegocioException("Un pedido solo puede incluir productos de un mismo productor");
        }
        return productos.get(0).getProductor();
    }

    private void validarDisponible(Producto producto) {
        boolean productorVerificado =
                producto.getProductor().getEstadoVerificacion() == EstadoVerificacion.VERIFICADO;

        if (!producto.isActivo() || !productorVerificado) {
            throw new ReglaNegocioException("El producto no está disponible: " + producto.getNombre());
        }
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findConDetallesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
    }

    private Productor productorDe(String email) {
        return productorRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de productor no encontrado"));
    }
}