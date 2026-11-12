package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Productor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductorRepository extends JpaRepository<Productor, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<Productor> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "usuario")
    List<Productor> findByEstadoVerificacionOrderByIdAsc(EstadoVerificacion estado);

    Optional<Productor> findByUsuarioEmail(String email);

    long countByEstadoVerificacion(EstadoVerificacion estado);
}
