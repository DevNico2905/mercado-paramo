package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
            select p from Producto p
            join fetch p.categoria c
            join fetch p.productor pr
            join fetch pr.usuario
            where p.activo = true
              and pr.estadoVerificacion = :estado
              and (:categoriaId is null or c.id = :categoriaId)
              and (:productorId is null or pr.id = :productorId)
            order by p.nombre
            """)
    List<Producto> buscarCatalogo(@Param("estado") EstadoVerificacion estado,
                                  @Param("categoriaId") Long categoriaId,
                                  @Param("productorId") Long productorId);

    @EntityGraph(attributePaths = {"categoria", "productor", "productor.usuario"})
    List<Producto> findByProductorIdOrderByIdAsc(Long productorId);

    @EntityGraph(attributePaths = {"categoria", "productor", "productor.usuario"})
    Optional<Producto> findConDetallesById(Long id);

    @EntityGraph(attributePaths = "productor")
    List<Producto> findByIdIn(Collection<Long> ids);
}
