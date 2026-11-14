package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.EstadoPedido;
import dev.nickbern.mercado_paramo.model.Pedido;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {"comprador", "productor", "productor.usuario", "detalles", "detalles.producto"})
    @Query("""
        select p from Pedido p
        where (:compradorEmail is null or p.comprador.email = :compradorEmail)
          and (:productorId is null or p.productor.id = :productorId)
          and (:estado is null or p.estado = :estado)
          and p.fecha between :inicio and :fin
        """)
    List<Pedido> buscar(@Param("compradorEmail") String compradorEmail,
                        @Param("productorId") Long productorId,
                        @Param("estado") EstadoPedido estado,
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fin") LocalDateTime fin,
                        Sort sort);

    @EntityGraph(attributePaths = {"comprador", "productor", "productor.usuario", "detalles", "detalles.producto"})
    Optional<Pedido> findConDetallesById(Long id);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    List<Pedido> findByProductorIdAndEstadoAndFechaBetween(
            Long productorId, EstadoPedido estado, LocalDateTime desde, LocalDateTime hasta);

    List<Pedido> findByEstado(EstadoPedido estado);

    long countByCompradorIdAndEstado(Long compradorId, EstadoPedido estado);
}