package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
