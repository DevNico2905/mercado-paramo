package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto,Long> {
}
