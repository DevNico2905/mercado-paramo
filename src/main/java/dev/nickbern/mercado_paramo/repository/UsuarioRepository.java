package dev.nickbern.mercado_paramo.repository;

import dev.nickbern.mercado_paramo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
