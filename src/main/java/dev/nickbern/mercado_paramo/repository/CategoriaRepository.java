package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
