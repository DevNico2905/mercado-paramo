package dev.nickbern.mercado_paramo.dto;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Rol;
import lombok.Builder;

@Builder
public record PerfilResponse(
        Long id,
        String nombre,
        String email,
        Rol rol,
        EstadoVerificacion estadoVerificacion
) {
}
