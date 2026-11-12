package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.EstadoPedido;
import dev.nickbern.mercado_paramo.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {"comprador", "productor", "productor.usuario", "detalles", "detalles.producto"})
    List<Pedido> findByCompradorEmailOrderByFechaDesc(String email);

    @EntityGraph(attributePaths = {"comprador", "productor", "productor.usuario", "detalles", "detalles.producto"})
    List<Pedido> findByProductorIdOrderByFechaDesc(Long productorId);

    @EntityGraph(attributePaths = {"comprador", "productor", "productor.usuario", "detalles", "detalles.producto"})
    List<Pedido> findByProductorIdAndEstadoOrderByFechaDesc(Long productorId, EstadoPedido estado);

    @EntityGraph(attributePaths = {"comprador", "productor", "productor.usuario", "detalles", "detalles.producto"})
    Optional<Pedido> findConDetallesById(Long id);
}